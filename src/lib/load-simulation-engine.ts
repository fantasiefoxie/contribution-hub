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

export interface HealthStatus {
  workerUtil: "healthy" | "warning" | "critical";
  queueBacklog: "healthy" | "warning" | "critical";
  p95Latency: "healthy" | "warning" | "critical";
  errorRate: "healthy" | "warning" | "critical";
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
      peak_users: 800,
      peak_minute: 60,
      spread: 35,
      volunteers: 2_000,
      validators: 50,
      tasks: 200,
      target: "4K households",
    },
  },
  city_survey: {
    infra: { api_servers: 4, bg_workers: 10, validators: 2_000, events_per_worker_per_second: 100, base_latency: 50, cache_efficiency: 0.92 },
    scenario: {
      name: "City Survey (50K)",
      peak_users: 20_000,
      peak_minute: 60,
      spread: 30,
      volunteers: 50_000,
      validators: 2_000,
      tasks: 5_000,
      target: "100K households",
    },
  },
  disaster_response: {
    infra: { api_servers: 6, bg_workers: 20, validators: 500, events_per_worker_per_second: 120, base_latency: 70, cache_efficiency: 0.85 },
    scenario: {
      name: "Disaster Response Surge",
      peak_users: 40_000,
      peak_minute: 30,
      spread: 15,
      volunteers: 80_000,
      validators: 500,
      tasks: 10_000,
      target: "200K households",
    },
  },
  national: {
    infra: { api_servers: 8, bg_workers: 25, validators: 5_000, events_per_worker_per_second: 150, base_latency: 55, cache_efficiency: 0.95 },
    scenario: {
      name: "National Campaign (200K)",
      peak_users: 80_000,
      peak_minute: 60,
      spread: 25,
      volunteers: 200_000,
      validators: 5_000,
      tasks: 25_000,
      target: "500K households",
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

export function generateTimeline(
  rawInfra: InfraParams,
  scenario: ScenarioConfig
): SimPoint[] {
  const infra = safeInfra(rawInfra);
  const points: SimPoint[] = [];
  const TIMESTEP_SEC = 5 * 60;

  // Safe scenario values
  const safeTasks = Math.max(1, scenario.tasks);
  const safeValidators = Math.max(1, scenario.validators);
  const safeSpread = Math.max(1, scenario.spread);

  let queueBacklog = 0;

  for (let m = 0; m <= 120; m += 5) {
    // 1. Traffic shape
    const t = (m - scenario.peak_minute) / safeSpread;
    const loadFactor = clamp(Math.exp(-0.5 * t * t), 0, 1);
    const concurrentUsers = Math.round(safeNum(loadFactor * scenario.peak_users));

    // 2. API RPS scales with servers
    const apiRps = Math.round(safeNum(concurrentUsers * 0.21 * infra.api_servers / 4));

    // 3. Event production
    const eventsProducedPerSec = apiRps * 0.25;
    const eventsProducedWindow = safeNum(eventsProducedPerSec * TIMESTEP_SEC);

    // 4. Worker capacity
    const workerCapacityPerSec = infra.bg_workers * infra.events_per_worker_per_second;
    const workerCapacityWindow = safeNum(workerCapacityPerSec * TIMESTEP_SEC);

    // 5. Drain — eventsProcessed never exceeds total queued
    const totalToDrain = queueBacklog + eventsProducedWindow;
    const eventsProcessed = Math.min(totalToDrain, workerCapacityWindow);

    // 6. Backlog update — never negative
    queueBacklog = Math.max(0, safeNum(totalToDrain - eventsProcessed));

    // 7. Worker util — clamped [0, 1]
    const workerUtil = workerCapacityWindow > 0
      ? clamp(eventsProcessed / workerCapacityWindow, 0, 1)
      : 0;

    // 8. Latency
    const cachePenalty = (1 - infra.cache_efficiency) * 200;
    const latencyMultiplier = 350;
    const backlogPenalty = 0.05;
    const p95 = Math.max(0, Math.round(safeNum(
      infra.base_latency + cachePenalty +
      workerUtil * latencyMultiplier +
      queueBacklog * backlogPenalty
    )));
    const p50 = Math.max(0, Math.round(p95 * 0.5));
    const p99 = Math.max(0, Math.round(p95 * 1.6));

    // 9. Error rate — clamped [0, 1]
    const errorRate = clamp(safeNum((p95 - 300) / 1000), 0, 1);

    // 10. DB & cache
    const dbCacheMiss = 1 - infra.cache_efficiency;
    const dbQps = Math.max(0, Math.round(safeNum(apiRps * (1.5 + dbCacheMiss * 6))));
    const cacheHitRatio = clamp(
      Math.round(safeNum(infra.cache_efficiency - workerUtil * 0.04) * 100) / 100,
      0.80, 0.99
    );
    const dbConnPool = clamp(Math.round(safeNum(
      (apiRps / (infra.api_servers * 25)) * 100 * 0.9
    )), 0, 100);

    // 11. Ops
    const validatorThroughputRatio = clamp(infra.validators / safeValidators, 0, 1);
    const taskClaims = Math.max(0, Math.round(safeNum(loadFactor * 380 * (safeTasks / 5000))));
    const submissions = Math.max(0, Math.round(safeNum(loadFactor * 210 * (safeTasks / 5000))));
    const validations = Math.max(0, Math.round(safeNum(loadFactor * 160 * validatorThroughputRatio * (safeTasks / 5000))));

    points.push({
      time: `${Math.floor(m / 60) + 18}:${String(m % 60).padStart(2, "0")}`,
      minute: m,
      concurrentUsers,
      apiRps,
      eventsProduced: Math.round(eventsProducedWindow),
      eventsProcessed: Math.round(eventsProcessed),
      eventQueueBacklog: Math.round(queueBacklog),
      workerUtil: Math.round(workerUtil * 100),
      p50, p95, p99,
      errorRate: Math.round(errorRate * 100) / 100,
      dbQps,
      cacheHitRatio,
      dbConnPool,
      taskClaims,
      submissions,
      validations,
    });
  }

  return points;
}
