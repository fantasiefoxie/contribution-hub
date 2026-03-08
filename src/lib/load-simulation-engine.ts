// ── Stateful System Dynamics Simulation Engine ──
// Production-hardened: all inputs validated, all outputs clamped, no NaN/Infinity possible.

/** Clamp a number between min and max */
function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

/** Return value if finite, otherwise fallback */
function safeNum(value: number, fallback: number = 0): number {
  return Number.isFinite(value) ? value : fallback;
}

/** Safe integer: round + clamp to non-negative */
function safeInt(value: number, fallback: number = 0): number {
  const n = safeNum(value, fallback);
  return Math.max(0, Math.round(n));
}

// ── Core Types ──

export interface InfraParams {
  readonly api_servers: number;
  readonly bg_workers: number;
  readonly validators: number;
  readonly events_per_worker_per_second: number;
  readonly base_latency: number;
  readonly cache_efficiency: number; // 0.80 – 0.99
}

export interface ScenarioConfig {
  readonly name: string;
  readonly peak_users: number;
  readonly peak_minute: number;
  readonly spread: number;
  readonly volunteers: number;
  readonly validators: number;
  readonly tasks: number;
  readonly target: string;
}

export interface SimPoint {
  readonly time: string;
  readonly minute: number;
  readonly concurrentUsers: number;
  readonly apiRps: number;
  readonly eventsProduced: number;
  readonly eventsProcessed: number;
  readonly eventQueueBacklog: number;
  readonly workerUtil: number;
  readonly p50: number;
  readonly p95: number;
  readonly p99: number;
  readonly errorRate: number;
  readonly dbQps: number;
  readonly cacheHitRatio: number;
  readonly dbConnPool: number;
  readonly taskClaims: number;
  readonly submissions: number;
  readonly validations: number;
}

export type HealthLevel = "healthy" | "warning" | "critical";

export interface HealthStatus {
  readonly workerUtil: HealthLevel;
  readonly queueBacklog: HealthLevel;
  readonly p95Latency: HealthLevel;
  readonly errorRate: HealthLevel;
}

export interface ScenarioSummary {
  readonly key: string;
  readonly scenario: ScenarioConfig;
  readonly infra: InfraParams;
  readonly timeline: readonly SimPoint[];
  readonly health: HealthStatus;
  readonly peakApiRps: number;
  readonly peakDbQps: number;
  readonly peakWorkerUtil: number;
  readonly peakBacklog: number;
  readonly peakP95: number;
  readonly peakP99: number;
  readonly peakErrorRate: number;
  readonly totalApiRequests: number;
  readonly recommendedInfra: InfraEstimate;
  readonly breakpoints: readonly Breakpoint[];
  readonly maxSafeVolunteers: number;
  readonly bottleneckComponent: string;
}

export interface InfraEstimate {
  readonly api_servers: number;
  readonly bg_workers: number;
  readonly redis_memory_gb: number;
  readonly db_replicas: number;
}

// ── Cost Model ──

export interface CostModel {
  readonly api_server_monthly: number;
  readonly worker_monthly: number;
  readonly postgres_primary: number;
  readonly postgres_replica: number;
  readonly redis_node: number;
  readonly storage_per_gb: number;
  readonly bandwidth_per_gb: number;
}

export const DEFAULT_COST_MODEL: CostModel = {
  api_server_monthly: 85,
  worker_monthly: 45,
  postgres_primary: 120,
  postgres_replica: 80,
  redis_node: 55,
  storage_per_gb: 0.10,
  bandwidth_per_gb: 0.08,
};

export interface CostBreakdown {
  readonly apiServers: number;
  readonly workers: number;
  readonly postgresPrimary: number;
  readonly postgresReplicas: number;
  readonly redis: number;
  readonly storage: number;
  readonly bandwidth: number;
  readonly total: number;
  readonly costPer1kVolunteers: number;
}

export interface CostOptimizationHint {
  readonly title: string;
  readonly description: string;
  readonly savingsPercent: number;
}

/** Sanitize cost model — enforce positive values */
function safeCostModel(cost: CostModel): CostModel {
  return {
    api_server_monthly: Math.max(0, safeNum(cost.api_server_monthly, 85)),
    worker_monthly: Math.max(0, safeNum(cost.worker_monthly, 45)),
    postgres_primary: Math.max(0, safeNum(cost.postgres_primary, 120)),
    postgres_replica: Math.max(0, safeNum(cost.postgres_replica, 80)),
    redis_node: Math.max(0, safeNum(cost.redis_node, 55)),
    storage_per_gb: Math.max(0, safeNum(cost.storage_per_gb, 0.10)),
    bandwidth_per_gb: Math.max(0, safeNum(cost.bandwidth_per_gb, 0.08)),
  };
}

/** Estimate monthly infrastructure cost — overflow-safe */
export function estimateCost(
  infra: InfraEstimate,
  scenario: ScenarioConfig,
  rawCost: CostModel = DEFAULT_COST_MODEL,
): CostBreakdown {
  const cost = safeCostModel(rawCost);
  const safeVol = Math.max(0, safeNum(scenario.volunteers));
  const safeTasks = Math.max(1, safeNum(scenario.tasks, 1));
  const safePeakUsers = Math.max(0, safeNum(scenario.peak_users));

  const storageGB = clamp(Math.ceil(safeVol * 0.002 * (safeTasks / 1000)), 1, 1_000_000);
  const bandwidthGB = clamp(Math.ceil(safePeakUsers * 0.05 * 30), 1, 10_000_000);

  const apiServers = safeNum(Math.max(1, infra.api_servers) * cost.api_server_monthly);
  const workers = safeNum(Math.max(1, infra.bg_workers) * cost.worker_monthly);
  const postgresPrimary = cost.postgres_primary;
  const postgresReplicas = safeNum(Math.max(0, infra.db_replicas) * cost.postgres_replica);
  const redis = safeNum(Math.max(1, Math.ceil(Math.max(1, infra.redis_memory_gb) / 4)) * cost.redis_node);
  const storage = safeNum(storageGB * cost.storage_per_gb);
  const bandwidth = safeNum(bandwidthGB * cost.bandwidth_per_gb);
  const total = safeNum(apiServers + workers + postgresPrimary + postgresReplicas + redis + storage + bandwidth);
  const costPer1k = safeVol > 0
    ? Math.round(safeNum((total / safeVol) * 1000) * 100) / 100
    : 0;

  return {
    apiServers, workers, postgresPrimary, postgresReplicas,
    redis, storage, bandwidth, total,
    costPer1kVolunteers: costPer1k,
  };
}

/** Generate cost optimization hints — safe against zero-total */
export function getCostHints(breakdown: CostBreakdown, scenario: ScenarioConfig): CostOptimizationHint[] {
  const hints: CostOptimizationHint[] = [];
  const total = Math.max(1, breakdown.total); // prevent divide-by-zero
  if (breakdown.workers / total > 0.35) {
    hints.push({ title: "Batch validation processing", description: "Schedule validations in batches instead of real-time to reduce worker count by ~30%.", savingsPercent: 12 });
  }
  if (breakdown.bandwidth / total > 0.05) {
    hints.push({ title: "Compress uploads", description: "Enable client-side image compression to reduce bandwidth costs.", savingsPercent: 8 });
  }
  if (breakdown.apiServers / total > 0.3) {
    hints.push({ title: "Increase cache TTL", description: "Extend cache TTL from 5min to 15min to reduce API server load.", savingsPercent: 15 });
  }
  if (safeNum(scenario.volunteers) > 50000 && breakdown.postgresReplicas > 0) {
    hints.push({ title: "Use read replicas strategically", description: "Route read-heavy queries to replicas to defer scaling the primary.", savingsPercent: 10 });
  }
  return hints;
}

export interface Breakpoint {
  readonly metric: string;
  readonly threshold: string;
  readonly actual: string;
  readonly severity: "warning" | "critical";
}

// ── Defaults ──

export const DEFAULT_INFRA: InfraParams = {
  api_servers: 4,
  bg_workers: 10,
  validators: 2_000,
  events_per_worker_per_second: 100,
  base_latency: 50,
  cache_efficiency: 0.92,
};

// ── Input Sanitizers ──

/** Enforce safe minimums on infra params — prevents divide-by-zero and NaN */
function safeInfra(infra: InfraParams): InfraParams {
  return {
    api_servers: clamp(safeNum(infra.api_servers, 1), 1, 100),
    bg_workers: clamp(safeNum(infra.bg_workers, 1), 1, 200),
    validators: clamp(safeNum(infra.validators, 1), 1, 100_000),
    events_per_worker_per_second: clamp(safeNum(infra.events_per_worker_per_second, 50), 1, 1_000),
    base_latency: clamp(safeNum(infra.base_latency, 10), 0, 2_000),
    cache_efficiency: clamp(safeNum(infra.cache_efficiency, 0.90), 0.01, 0.99),
  };
}

/** Enforce safe minimums on scenario params */
function safeScenario(s: ScenarioConfig): ScenarioConfig {
  return {
    name: (s.name || "Unnamed").slice(0, 100),
    peak_users: clamp(safeNum(s.peak_users, 100), 0, 10_000_000),
    peak_minute: clamp(safeNum(s.peak_minute, 60), 0, 120),
    spread: clamp(safeNum(s.spread, 30), 1, 120),
    volunteers: clamp(safeNum(s.volunteers, 100), 0, 10_000_000),
    validators: clamp(safeNum(s.validators, 10), 1, 100_000),
    tasks: clamp(safeNum(s.tasks, 10), 1, 1_000_000),
    target: (s.target || "—").slice(0, 100),
  };
}

// ── Preset Scenarios ──

export const PRESETS: Readonly<Record<string, { infra: Partial<InfraParams>; scenario: ScenarioConfig }>> = {
  small_ngo: {
    infra: { api_servers: 2, bg_workers: 3, validators: 50, events_per_worker_per_second: 80, base_latency: 60, cache_efficiency: 0.88 },
    scenario: { name: "Small NGO Campaign", peak_users: 800, peak_minute: 60, spread: 35, volunteers: 2_000, validators: 50, tasks: 200, target: "4K households" },
  },
  city_survey_10k: {
    infra: { api_servers: 3, bg_workers: 6, validators: 500, events_per_worker_per_second: 90, base_latency: 55, cache_efficiency: 0.90 },
    scenario: { name: "City Survey (10K)", peak_users: 4_000, peak_minute: 60, spread: 30, volunteers: 10_000, validators: 500, tasks: 1_500, target: "25K households" },
  },
  city_survey: {
    infra: { api_servers: 4, bg_workers: 10, validators: 2_000, events_per_worker_per_second: 100, base_latency: 50, cache_efficiency: 0.92 },
    scenario: { name: "City Survey (50K)", peak_users: 20_000, peak_minute: 60, spread: 30, volunteers: 50_000, validators: 2_000, tasks: 5_000, target: "100K households" },
  },
  disaster_response: {
    infra: { api_servers: 6, bg_workers: 20, validators: 500, events_per_worker_per_second: 120, base_latency: 70, cache_efficiency: 0.85 },
    scenario: { name: "Disaster Response Surge", peak_users: 40_000, peak_minute: 30, spread: 15, volunteers: 80_000, validators: 500, tasks: 10_000, target: "200K households" },
  },
  national: {
    infra: { api_servers: 8, bg_workers: 25, validators: 5_000, events_per_worker_per_second: 150, base_latency: 55, cache_efficiency: 0.95 },
    scenario: { name: "National Campaign (200K)", peak_users: 80_000, peak_minute: 60, spread: 25, volunteers: 200_000, validators: 5_000, tasks: 25_000, target: "500K households" },
  },
  global_network: {
    infra: { api_servers: 16, bg_workers: 50, validators: 10_000, events_per_worker_per_second: 200, base_latency: 65, cache_efficiency: 0.96 },
    scenario: { name: "Global Network (1M)", peak_users: 300_000, peak_minute: 60, spread: 40, volunteers: 1_000_000, validators: 10_000, tasks: 100_000, target: "2M households" },
  },
};

export const DEFAULT_SCENARIO: ScenarioConfig = PRESETS.city_survey.scenario;

// ── Analysis Functions ──

const DEFAULT_HEALTH: HealthStatus = { workerUtil: "healthy", queueBacklog: "healthy", p95Latency: "healthy", errorRate: "healthy" };

export function computeHealth(timeline: readonly SimPoint[]): HealthStatus {
  if (timeline.length === 0) return DEFAULT_HEALTH;
  const peakWorker = Math.max(...timeline.map((p) => p.workerUtil));
  const peakBacklog = Math.max(...timeline.map((p) => p.eventQueueBacklog));
  const peakP95 = Math.max(...timeline.map((p) => p.p95));
  const peakErr = Math.max(...timeline.map((p) => p.errorRate));
  return {
    workerUtil: peakWorker >= 95 ? "critical" : peakWorker > 75 ? "warning" : "healthy",
    queueBacklog: peakBacklog > 2000 ? "critical" : peakBacklog > 500 ? "warning" : "healthy",
    p95Latency: peakP95 > 500 ? "critical" : peakP95 > 250 ? "warning" : "healthy",
    errorRate: peakErr > 0.05 ? "critical" : peakErr > 0.01 ? "warning" : "healthy",
  };
}

/** Estimate minimum infra for a scenario at comfortable headroom */
export function estimateInfra(rawScenario: ScenarioConfig): InfraEstimate {
  const scenario = safeScenario(rawScenario);
  const peakRps = safeInt(scenario.peak_users * 0.21);
  const apiServers = Math.max(2, Math.ceil(peakRps / 500));
  const eventsPerSec = peakRps * 0.25;
  const bgWorkers = Math.max(2, Math.ceil(eventsPerSec / 80));
  const redisGb = Math.max(1, Math.ceil(scenario.volunteers / 50_000) * 2);
  const dbReplicas = scenario.peak_users > 30_000 ? Math.ceil(scenario.peak_users / 40_000) : 0;
  return { api_servers: apiServers, bg_workers: bgWorkers, redis_memory_gb: redisGb, db_replicas: dbReplicas };
}

/** Detect scaling breakpoints in a timeline */
export function detectBreakpoints(timeline: readonly SimPoint[]): Breakpoint[] {
  if (timeline.length === 0) return [];
  const bp: Breakpoint[] = [];
  const peakWorker = Math.max(...timeline.map((p) => p.workerUtil));
  const peakP95 = Math.max(...timeline.map((p) => p.p95));
  const peakBacklog = Math.max(...timeline.map((p) => p.eventQueueBacklog));
  const peakErr = Math.max(...timeline.map((p) => p.errorRate));

  if (peakWorker > 85) bp.push({ metric: "Worker Utilization", threshold: "85%", actual: `${peakWorker}%`, severity: peakWorker >= 95 ? "critical" : "warning" });
  if (peakP95 > 400) bp.push({ metric: "p95 Latency", threshold: "400ms", actual: `${peakP95}ms`, severity: peakP95 > 600 ? "critical" : "warning" });
  if (peakBacklog > 1000) bp.push({ metric: "Queue Backlog", threshold: "1,000", actual: peakBacklog.toLocaleString(), severity: peakBacklog > 2000 ? "critical" : "warning" });
  if (peakErr > 0.02) bp.push({ metric: "Error Rate", threshold: "2%", actual: `${(peakErr * 100).toFixed(1)}%`, severity: peakErr > 0.05 ? "critical" : "warning" });
  return bp;
}

/** Summarize a full scenario simulation — fault-tolerant, never throws */
export function summarizeScenario(key: string, presetKey?: string): ScenarioSummary {
  const preset = presetKey ? PRESETS[presetKey] : PRESETS[key];
  if (!preset) {
    // Fallback to default scenario instead of throwing
    const fallbackInfra = DEFAULT_INFRA;
    const fallbackScenario: ScenarioConfig = { ...DEFAULT_SCENARIO, name: `Unknown (${key})` };
    return buildSummary(key, fallbackInfra, fallbackScenario);
  }
  const infra: InfraParams = { ...DEFAULT_INFRA, ...preset.infra };
  return buildSummary(key, infra, preset.scenario);
}

function buildSummary(key: string, infra: InfraParams, scenario: ScenarioConfig): ScenarioSummary {
  const timeline = generateTimeline(infra, scenario);
  const health = computeHealth(timeline);
  const breakpoints = detectBreakpoints(timeline);
  const recommendedInfra = estimateInfra(scenario);

  const peakApiRps = timeline.length > 0 ? Math.max(...timeline.map((p) => p.apiRps)) : 0;
  const peakDbQps = timeline.length > 0 ? Math.max(...timeline.map((p) => p.dbQps)) : 0;
  const peakWorkerUtil = timeline.length > 0 ? Math.max(...timeline.map((p) => p.workerUtil)) : 0;
  const peakBacklog = timeline.length > 0 ? Math.max(...timeline.map((p) => p.eventQueueBacklog)) : 0;
  const peakP95 = timeline.length > 0 ? Math.max(...timeline.map((p) => p.p95)) : 0;
  const peakP99 = timeline.length > 0 ? Math.max(...timeline.map((p) => p.p99)) : 0;
  const peakErrorRate = timeline.length > 0 ? Math.max(...timeline.map((p) => p.errorRate)) : 0;
  const totalApiRequests = timeline.reduce((s, p) => s + p.apiRps * 5 * 60, 0);

  let bottleneckComponent = "None";
  if (peakWorkerUtil >= 95) bottleneckComponent = "Background Workers";
  else if (peakP95 > 500) bottleneckComponent = "API Servers / Latency";
  else if (peakBacklog > 2000) bottleneckComponent = "Event Queue";
  else if (peakErrorRate > 0.05) bottleneckComponent = "Error Cascade";

  let maxSafe = scenario.volunteers;
  if (breakpoints.some((b) => b.severity === "critical")) {
    maxSafe = Math.round(scenario.volunteers * 0.6);
  } else if (breakpoints.length > 0) {
    maxSafe = Math.round(scenario.volunteers * 0.85);
  }

  return {
    key, scenario, infra, timeline, health,
    peakApiRps, peakDbQps, peakWorkerUtil, peakBacklog,
    peakP95, peakP99, peakErrorRate, totalApiRequests,
    recommendedInfra, breakpoints,
    maxSafeVolunteers: maxSafe, bottleneckComponent,
  };
}

// ── Timeline Generator ──

/** Maximum number of time steps to prevent runaway loops */
const MAX_TIMESTEPS = 50;

export function generateTimeline(
  rawInfra: InfraParams,
  rawScenario: ScenarioConfig,
): SimPoint[] {
  const infra = safeInfra(rawInfra);
  const scenario = safeScenario(rawScenario);
  const points: SimPoint[] = [];
  const TIMESTEP_SEC = 5 * 60;
  const safeTasks = Math.max(1, scenario.tasks);
  const safeValidators = Math.max(1, scenario.validators);

  let queueBacklog = 0;
  let stepCount = 0;

  for (let m = 0; m <= 120; m += 5) {
    if (++stepCount > MAX_TIMESTEPS) break; // hard safety limit

    const t = (m - scenario.peak_minute) / scenario.spread; // spread already >= 1 via safeScenario
    const loadFactor = clamp(safeNum(Math.exp(-0.5 * t * t)), 0, 1);
    const concurrentUsers = safeInt(loadFactor * scenario.peak_users);
    const apiRps = safeInt(concurrentUsers * 0.21 * infra.api_servers / 4);
    const eventsProducedPerSec = apiRps * 0.25;
    const eventsProducedWindow = safeNum(eventsProducedPerSec * TIMESTEP_SEC);
    const workerCapacityPerSec = infra.bg_workers * infra.events_per_worker_per_second;
    const workerCapacityWindow = safeNum(workerCapacityPerSec * TIMESTEP_SEC);
    const totalToDrain = safeNum(queueBacklog + eventsProducedWindow);
    const eventsProcessed = Math.min(totalToDrain, workerCapacityWindow);
    queueBacklog = Math.max(0, safeNum(totalToDrain - eventsProcessed));

    // Cap backlog to prevent unbounded growth in extreme scenarios
    queueBacklog = Math.min(queueBacklog, 1_000_000);

    const workerUtil = workerCapacityWindow > 0
      ? clamp(safeNum(eventsProcessed / workerCapacityWindow), 0, 1)
      : 0;

    const cachePenalty = (1 - infra.cache_efficiency) * 200;
    const rawP95 = infra.base_latency + cachePenalty + workerUtil * 350 + queueBacklog * 0.05;
    const p95 = clamp(safeInt(rawP95), 0, 99_999);
    const p50 = clamp(Math.round(p95 * 0.5), 0, 99_999);
    const p99 = clamp(Math.round(p95 * 1.6), 0, 99_999);
    const errorRate = clamp(safeNum((p95 - 300) / 1000), 0, 1);

    const dbCacheMiss = 1 - infra.cache_efficiency;
    const dbQps = safeInt(apiRps * (1.5 + dbCacheMiss * 6));
    const cacheHitRatio = clamp(
      Math.round(safeNum(infra.cache_efficiency - workerUtil * 0.04) * 100) / 100, 0.80, 0.99,
    );
    const dbConnPool = clamp(safeInt(
      (apiRps / (infra.api_servers * 25)) * 100 * 0.9,
    ), 0, 100);

    const validatorThroughputRatio = clamp(safeNum(infra.validators / safeValidators), 0, 1);
    const taskClaims = safeInt(loadFactor * 380 * (safeTasks / 5000));
    const submissions = safeInt(loadFactor * 210 * (safeTasks / 5000));
    const validations = safeInt(loadFactor * 160 * validatorThroughputRatio * (safeTasks / 5000));

    points.push({
      time: `${Math.floor(m / 60) + 18}:${String(m % 60).padStart(2, "0")}`,
      minute: m, concurrentUsers, apiRps,
      eventsProduced: Math.round(eventsProducedWindow),
      eventsProcessed: Math.round(eventsProcessed),
      eventQueueBacklog: Math.round(queueBacklog),
      workerUtil: Math.round(workerUtil * 100),
      p50, p95, p99,
      errorRate: Math.round(errorRate * 100) / 100,
      dbQps, cacheHitRatio, dbConnPool,
      taskClaims, submissions, validations,
    });
  }

  return points;
}
