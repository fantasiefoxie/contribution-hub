// ── Stateful System Dynamics Simulation Engine ──

export interface InfraParams {
  api_servers: number;
  bg_workers: number;
  events_per_worker_per_second: number;
  base_latency: number;
}

export interface ScenarioConfig {
  peak_users: number;
  peak_minute: number;
  spread: number;
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

export const DEFAULT_INFRA: InfraParams = {
  api_servers: 4,
  bg_workers: 10,
  events_per_worker_per_second: 8,
  base_latency: 25,
};

export const DEFAULT_SCENARIO: ScenarioConfig = {
  peak_users: 20_000,
  peak_minute: 60,
  spread: 30,
};

export function generateTimeline(
  infra: InfraParams,
  scenario: ScenarioConfig
): SimPoint[] {
  const points: SimPoint[] = [];
  const TIMESTEP_SEC = 5 * 60; // 5-minute windows

  // ── Persistent state variables ──
  let queueBacklog = 0;

  for (let m = 0; m <= 120; m += 5) {
    // ── 1. Traffic shape (bell curve) ──
    const t = (m - scenario.peak_minute) / scenario.spread;
    const loadFactor = Math.exp(-0.5 * t * t);

    const concurrentUsers = Math.round(loadFactor * scenario.peak_users);

    // ── 2. API request rate from concurrent users ──
    // Each user generates ~0.21 RPS (1 req every ~4.7s avg session activity)
    const apiRps = Math.round(concurrentUsers * 0.21 * infra.api_servers / 4);

    // ── 3. Event production from API traffic ──
    const eventsProducedPerSec = apiRps * 0.25;
    const eventsProducedWindow = eventsProducedPerSec * TIMESTEP_SEC;

    // ── 4. Worker processing capacity ──
    const workerCapacityPerSec = infra.bg_workers * infra.events_per_worker_per_second;
    const workerCapacityWindow = workerCapacityPerSec * TIMESTEP_SEC;

    // ── 5. Events actually processed (capped by capacity + backlog) ──
    const totalToDrain = queueBacklog + eventsProducedWindow;
    const eventsProcessed = Math.min(totalToDrain, workerCapacityWindow);

    // ── 6. Update queue backlog (system dynamics) ──
    queueBacklog = Math.max(0, totalToDrain - eventsProcessed);

    // ── 7. Worker utilization ──
    const workerUtil = workerCapacityWindow > 0
      ? Math.min(1, eventsProcessed / workerCapacityWindow)
      : 0;

    // ── 8. Latency model (cascading from utilization + backlog) ──
    const latencyMultiplier = 350;
    const backlogPenalty = 0.05;
    const p95 = Math.round(
      infra.base_latency +
      workerUtil * latencyMultiplier +
      queueBacklog * backlogPenalty
    );
    const p50 = Math.round(p95 * 0.5);
    const p99 = Math.round(p95 * 1.6);

    // ── 9. Error rate (rises when latency exceeds threshold) ──
    const errorRate = Math.min(1, Math.max(0, (p95 - 300) / 1000));

    // ── 10. Derived DB & cache metrics ──
    const dbQps = Math.round(apiRps * 2.8);
    const cacheHitRatio = Math.round(Math.min(0.97, 0.92 - workerUtil * 0.06) * 100) / 100;
    const dbConnPool = Math.min(100, Math.round(
      (apiRps / (infra.api_servers * 25)) * 100 * 0.9
    ));

    // ── 11. Operational counters (proportional to traffic) ──
    const taskClaims = Math.round(loadFactor * 380);
    const submissions = Math.round(loadFactor * 210);
    const validations = Math.round(loadFactor * 160);

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
