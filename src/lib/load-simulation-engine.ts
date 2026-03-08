// ── Stateful System Dynamics Simulation Engine ──

/** Clamp a number between min and max */
function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

/** Return value if finite, otherwise fallback */
function safeNum(value: number, fallback: number = 0): number {
  return Number.isFinite(value) ? value : fallback;
}

export interface InfraParams {
  api_servers: number;
  bg_workers: number;
  validators: number;
  events_per_worker_per_second: number;
  base_latency: number;
  cache_efficiency: number; // 0.80 – 0.99
}

export interface ScenarioConfig {
  name: string;
  peak_users: number;
  peak_minute: number;
  spread: number;
  volunteers: number;
  validators: number;
  tasks: number;
  target: string;
}

export interface SimPoint {
  time: string;
  minute: number;
  concurrentUsers: number;
  apiRps: number;
  eventsProduced: number;
  eventsProcessed: number;
  eventQueueBacklog: number;
  workerUtil: number;
  p50: number;
  p95: number;
  p99: number;
  errorRate: number;
  dbQps: number;
  cacheHitRatio: number;
  dbConnPool: number;
  taskClaims: number;
  submissions: number;
  validations: number;
}

export type HealthLevel = "healthy" | "warning" | "critical";

export interface HealthStatus {
  workerUtil: HealthLevel;
  queueBacklog: HealthLevel;
  p95Latency: HealthLevel;
  errorRate: HealthLevel;
}

/** Summary of a fully-simulated scenario — used for comparison tables */
export interface ScenarioSummary {
  key: string;
  scenario: ScenarioConfig;
  infra: InfraParams;
  timeline: SimPoint[];
  health: HealthStatus;
  peakApiRps: number;
  peakDbQps: number;
  peakWorkerUtil: number;
  peakBacklog: number;
  peakP95: number;
  peakP99: number;
  peakErrorRate: number;
  totalApiRequests: number;
  recommendedInfra: InfraEstimate;
  breakpoints: Breakpoint[];
  maxSafeVolunteers: number;
  bottleneckComponent: string;
}

export interface InfraEstimate {
  api_servers: number;
  bg_workers: number;
  redis_memory_gb: number;
  db_replicas: number;
}

export interface Breakpoint {
  metric: string;
  threshold: string;
  actual: string;
  severity: "warning" | "critical";
}

export const DEFAULT_INFRA: InfraParams = {
  api_servers: 4,
  bg_workers: 10,
  validators: 2_000,
  events_per_worker_per_second: 100,
  base_latency: 50,
  cache_efficiency: 0.92,
};

// ── Safe Infra: enforce minimums to prevent divide-by-zero ──
function safeInfra(infra: InfraParams): InfraParams {
  return {
    api_servers: Math.max(1, infra.api_servers),
    bg_workers: Math.max(1, infra.bg_workers),
    validators: Math.max(1, infra.validators),
    events_per_worker_per_second: Math.max(1, infra.events_per_worker_per_second),
    base_latency: Math.max(0, infra.base_latency),
    cache_efficiency: clamp(infra.cache_efficiency, 0.01, 0.99),
  };
}

// ── Preset Scenarios ──
export const PRESETS: Record<string, { infra: Partial<InfraParams>; scenario: ScenarioConfig }> = {
  small_ngo: {
    infra: { api_servers: 2, bg_workers: 3, validators: 50, events_per_worker_per_second: 80, base_latency: 60, cache_efficiency: 0.88 },
    scenario: {
      name: "Small NGO Campaign",
      peak_users: 800, peak_minute: 60, spread: 35,
      volunteers: 2_000, validators: 50, tasks: 200, target: "4K households",
    },
  },
  city_survey_10k: {
    infra: { api_servers: 3, bg_workers: 6, validators: 500, events_per_worker_per_second: 90, base_latency: 55, cache_efficiency: 0.90 },
    scenario: {
      name: "City Survey (10K)",
      peak_users: 4_000, peak_minute: 60, spread: 30,
      volunteers: 10_000, validators: 500, tasks: 1_500, target: "25K households",
    },
  },
  city_survey: {
    infra: { api_servers: 4, bg_workers: 10, validators: 2_000, events_per_worker_per_second: 100, base_latency: 50, cache_efficiency: 0.92 },
    scenario: {
      name: "City Survey (50K)",
      peak_users: 20_000, peak_minute: 60, spread: 30,
      volunteers: 50_000, validators: 2_000, tasks: 5_000, target: "100K households",
    },
  },
  disaster_response: {
    infra: { api_servers: 6, bg_workers: 20, validators: 500, events_per_worker_per_second: 120, base_latency: 70, cache_efficiency: 0.85 },
    scenario: {
      name: "Disaster Response Surge",
      peak_users: 40_000, peak_minute: 30, spread: 15,
      volunteers: 80_000, validators: 500, tasks: 10_000, target: "200K households",
    },
  },
  national: {
    infra: { api_servers: 8, bg_workers: 25, validators: 5_000, events_per_worker_per_second: 150, base_latency: 55, cache_efficiency: 0.95 },
    scenario: {
      name: "National Campaign (200K)",
      peak_users: 80_000, peak_minute: 60, spread: 25,
      volunteers: 200_000, validators: 5_000, tasks: 25_000, target: "500K households",
    },
  },
  global_network: {
    infra: { api_servers: 16, bg_workers: 50, validators: 10_000, events_per_worker_per_second: 200, base_latency: 65, cache_efficiency: 0.96 },
    scenario: {
      name: "Global Network (1M)",
      peak_users: 300_000, peak_minute: 60, spread: 40,
      volunteers: 1_000_000, validators: 10_000, tasks: 100_000, target: "2M households",
    },
  },
};

export const DEFAULT_SCENARIO: ScenarioConfig = PRESETS.city_survey.scenario;

export function computeHealth(timeline: SimPoint[]): HealthStatus {
  if (timeline.length === 0) {
    return { workerUtil: "healthy", queueBacklog: "healthy", p95Latency: "healthy", errorRate: "healthy" };
  }
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
export function estimateInfra(scenario: ScenarioConfig): InfraEstimate {
  const peakRps = Math.round(scenario.peak_users * 0.21);
  const apiServers = Math.max(2, Math.ceil(peakRps / 500));
  const eventsPerSec = peakRps * 0.25;
  const bgWorkers = Math.max(2, Math.ceil(eventsPerSec / 80)); // 80% target util
  const redisGb = Math.max(1, Math.ceil(scenario.volunteers / 50_000) * 2);
  const dbReplicas = scenario.peak_users > 30_000 ? Math.ceil(scenario.peak_users / 40_000) : 0;
  return { api_servers: apiServers, bg_workers: bgWorkers, redis_memory_gb: redisGb, db_replicas: dbReplicas };
}

/** Detect scaling breakpoints in a timeline */
export function detectBreakpoints(timeline: SimPoint[]): Breakpoint[] {
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

/** Summarize a full scenario simulation */
export function summarizeScenario(key: string, presetKey?: string): ScenarioSummary {
  const preset = presetKey ? PRESETS[presetKey] : PRESETS[key];
  if (!preset) throw new Error(`Unknown preset: ${key}`);
  const infra: InfraParams = { ...DEFAULT_INFRA, ...preset.infra };
  const scenario = preset.scenario;
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

  // Determine bottleneck
  let bottleneckComponent = "None";
  if (peakWorkerUtil >= 95) bottleneckComponent = "Background Workers";
  else if (peakP95 > 500) bottleneckComponent = "API Servers / Latency";
  else if (peakBacklog > 2000) bottleneckComponent = "Event Queue";
  else if (peakErrorRate > 0.05) bottleneckComponent = "Error Cascade";

  // Estimate max safe volunteers (binary search approach simplified)
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

export function generateTimeline(
  rawInfra: InfraParams,
  scenario: ScenarioConfig
): SimPoint[] {
  const infra = safeInfra(rawInfra);
  const points: SimPoint[] = [];
  const TIMESTEP_SEC = 5 * 60;
  const safeTasks = Math.max(1, scenario.tasks);
  const safeValidators = Math.max(1, scenario.validators);
  const safeSpread = Math.max(1, scenario.spread);

  let queueBacklog = 0;

  for (let m = 0; m <= 120; m += 5) {
    const t = (m - scenario.peak_minute) / safeSpread;
    const loadFactor = clamp(Math.exp(-0.5 * t * t), 0, 1);
    const concurrentUsers = Math.round(safeNum(loadFactor * scenario.peak_users));
    const apiRps = Math.round(safeNum(concurrentUsers * 0.21 * infra.api_servers / 4));
    const eventsProducedPerSec = apiRps * 0.25;
    const eventsProducedWindow = safeNum(eventsProducedPerSec * TIMESTEP_SEC);
    const workerCapacityPerSec = infra.bg_workers * infra.events_per_worker_per_second;
    const workerCapacityWindow = safeNum(workerCapacityPerSec * TIMESTEP_SEC);
    const totalToDrain = queueBacklog + eventsProducedWindow;
    const eventsProcessed = Math.min(totalToDrain, workerCapacityWindow);
    queueBacklog = Math.max(0, safeNum(totalToDrain - eventsProcessed));

    const workerUtil = workerCapacityWindow > 0
      ? clamp(eventsProcessed / workerCapacityWindow, 0, 1)
      : 0;

    const cachePenalty = (1 - infra.cache_efficiency) * 200;
    const p95 = Math.max(0, Math.round(safeNum(
      infra.base_latency + cachePenalty + workerUtil * 350 + queueBacklog * 0.05
    )));
    const p50 = Math.max(0, Math.round(p95 * 0.5));
    const p99 = Math.max(0, Math.round(p95 * 1.6));
    const errorRate = clamp(safeNum((p95 - 300) / 1000), 0, 1);

    const dbCacheMiss = 1 - infra.cache_efficiency;
    const dbQps = Math.max(0, Math.round(safeNum(apiRps * (1.5 + dbCacheMiss * 6))));
    const cacheHitRatio = clamp(
      Math.round(safeNum(infra.cache_efficiency - workerUtil * 0.04) * 100) / 100, 0.80, 0.99
    );
    const dbConnPool = clamp(Math.round(safeNum(
      (apiRps / (infra.api_servers * 25)) * 100 * 0.9
    )), 0, 100);

    const validatorThroughputRatio = clamp(infra.validators / safeValidators, 0, 1);
    const taskClaims = Math.max(0, Math.round(safeNum(loadFactor * 380 * (safeTasks / 5000))));
    const submissions = Math.max(0, Math.round(safeNum(loadFactor * 210 * (safeTasks / 5000))));
    const validations = Math.max(0, Math.round(safeNum(loadFactor * 160 * validatorThroughputRatio * (safeTasks / 5000))));

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
