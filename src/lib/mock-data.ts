/**
 * ── Centralized Mock Data ──
 *
 * Comprehensive fallback data used when the backend returns empty or errors.
 * Every entity shape matches what the edge functions return so that
 * components never encounter undefined fields.
 *
 * ## Adding new mocks
 * 1. Define the data here with realistic values
 * 2. Export it with a descriptive name
 * 3. Use it in the relevant hook via `data ?? mockXyz`
 */

// ─── Helpers ───
const ago = (mins: number) => new Date(Date.now() - mins * 60_000).toISOString();
const future = (mins: number) => new Date(Date.now() + mins * 60_000).toISOString();

// ─── Users / Profiles ───
export const mockProfiles = [
  { id: "user-1", display_name: "Rahul D", avatar_url: null, status: "trusted", interests: ["environment", "community", "education"], latitude: 19.033, longitude: 73.029 },
  { id: "user-2", display_name: "Maria Santos", avatar_url: null, status: "registered", interests: ["environment", "outdoor"], latitude: 19.076, longitude: 72.878 },
  { id: "user-3", display_name: "James Park", avatar_url: null, status: "trusted", interests: ["research", "community"], latitude: 19.12, longitude: 72.85 },
  { id: "user-4", display_name: "Aisha Johnson", avatar_url: null, status: "registered", interests: ["environment", "research"], latitude: 19.08, longitude: 72.89 },
  { id: "user-5", display_name: "Priya Sharma", avatar_url: null, status: "trusted", interests: ["education", "community"], latitude: 19.05, longitude: 72.83 },
] as const;

// ─── Tasks ───
export const mockTasks = [
  {
    id: "demo-1",
    title: "Community Garden Cleanup",
    description: "Help maintain the local community garden by weeding, watering, and planting new seedlings. All supplies provided at the meeting point near the south entrance.",
    difficulty: 2,
    status: "open",
    tags: ["environment", "outdoor"],
    visibility: "public",
    is_team: true,
    team_target: 5,
    is_priority: false,
    lock_duration_mins: 60,
    created_at: ago(2880),
    updated_at: ago(120),
    expires_at: null,
    created_by: "user-1",
    org_id: "org-1",
    initiative_id: "init-1",
    slug: "community-garden-cleanup",
    latitude: 19.033,
    longitude: 73.029,
  },
  {
    id: "demo-2",
    title: "Survey Local Businesses",
    description: "Map and document local businesses for the community directory. Visit businesses on Main Street, collect information about hours, services, and contact details.",
    difficulty: 3,
    status: "open",
    tags: ["research", "community"],
    visibility: "public",
    is_team: false,
    team_target: null,
    is_priority: false,
    lock_duration_mins: 120,
    created_at: ago(1440),
    updated_at: ago(60),
    expires_at: future(4320),
    created_by: "user-3",
    org_id: "org-1",
    initiative_id: "init-2",
    slug: "survey-local-businesses",
    latitude: 19.076,
    longitude: 72.878,
  },
  {
    id: "demo-3",
    title: "Youth Coding Workshop Helper",
    description: "Mentor students during a weekend coding workshop. Help beginners learn HTML/CSS and guide intermediate students through JavaScript projects.",
    difficulty: 4,
    status: "open",
    tags: ["community", "education"],
    visibility: "public",
    is_team: true,
    team_target: 3,
    is_priority: true,
    lock_duration_mins: 180,
    created_at: ago(720),
    updated_at: ago(30),
    expires_at: future(1440),
    created_by: "user-5",
    org_id: "org-2",
    initiative_id: "init-3",
    slug: "youth-coding-workshop-helper",
    latitude: 19.12,
    longitude: 72.85,
  },
  {
    id: "demo-4",
    title: "River Water Quality Testing",
    description: "Collect water samples from designated stations along the river for environmental analysis. Training and equipment provided.",
    difficulty: 3,
    status: "reserved",
    tags: ["environment", "research"],
    visibility: "public",
    is_team: false,
    team_target: null,
    is_priority: false,
    lock_duration_mins: 90,
    created_at: ago(4320),
    updated_at: ago(200),
    expires_at: null,
    created_by: "user-4",
    org_id: "org-1",
    initiative_id: "init-1",
    slug: "river-water-quality-testing",
    latitude: 19.05,
    longitude: 72.83,
  },
  {
    id: "demo-5",
    title: "Translate Health Flyers",
    description: "Translate community health flyers from English to Hindi and Marathi. Digital-only task — work from anywhere.",
    difficulty: 1,
    status: "open",
    tags: ["community", "online"],
    visibility: "public",
    is_team: false,
    team_target: null,
    is_priority: false,
    lock_duration_mins: 60,
    created_at: ago(360),
    updated_at: ago(10),
    expires_at: future(10080),
    created_by: "user-2",
    org_id: "org-2",
    initiative_id: null,
    slug: "translate-health-flyers",
    latitude: null,
    longitude: null,
  },
  {
    id: "demo-6",
    title: "Organize Food Drive Collection",
    description: "Help organize and sort donated food items at the community centre. Tasks include labelling, sorting by type, and packing boxes.",
    difficulty: 2,
    status: "claimed",
    tags: ["community", "logistics"],
    visibility: "public",
    is_team: true,
    team_target: 8,
    is_priority: true,
    lock_duration_mins: 120,
    created_at: ago(5760),
    updated_at: ago(45),
    expires_at: future(2880),
    created_by: "user-1",
    org_id: "org-1",
    initiative_id: "init-2",
    slug: "organize-food-drive-collection",
    latitude: 19.09,
    longitude: 72.86,
  },
  {
    id: "demo-7",
    title: "Paint School Boundary Wall",
    description: "Join the mural painting team to beautify the local primary school wall. Art supplies and lunch provided.",
    difficulty: 2,
    status: "in_progress",
    tags: ["community", "outdoor", "art"],
    visibility: "public",
    is_team: true,
    team_target: 10,
    is_priority: false,
    lock_duration_mins: 240,
    created_at: ago(10080),
    updated_at: ago(90),
    expires_at: future(1440),
    created_by: "user-3",
    org_id: "org-2",
    initiative_id: "init-3",
    slug: "paint-school-boundary-wall",
    latitude: 19.1,
    longitude: 72.87,
  },
  {
    id: "demo-8",
    title: "Data Entry for Census Records",
    description: "Digitize paper census records into a shared spreadsheet. Online task requiring attention to detail.",
    difficulty: 1,
    status: "open",
    tags: ["research", "online"],
    visibility: "public",
    is_team: false,
    team_target: null,
    is_priority: false,
    lock_duration_mins: 60,
    created_at: ago(240),
    updated_at: ago(5),
    expires_at: future(20160),
    created_by: "user-5",
    org_id: "org-1",
    initiative_id: "init-2",
    slug: "data-entry-census-records",
    latitude: null,
    longitude: null,
  },
];

// ─── Organizations ───
export const mockOrganizations = [
  { id: "org-1", name: "Green City Initiative", slug: "green-city", description: "Urban sustainability and community development", logo_url: null },
  { id: "org-2", name: "Youth Empowerment Network", slug: "youth-empower", description: "Education and youth skill-building programs", logo_url: null },
];

// ─── Initiatives ───
export const mockInitiatives = [
  {
    id: "init-1",
    title: "River Cleanup Campaign",
    slug: "river-cleanup-campaign",
    description: "A city-wide campaign to clean and restore the local river ecosystem.",
    status: "active",
    category: "environment",
    is_online: false,
    is_micro: false,
    created_by: "user-1",
    volunteer_target: 200,
    priority_score: 85,
    start_date: "2026-01-15",
    end_date: "2026-06-30",
    created_at: ago(43200),
    region_name: "Navi Mumbai",
  },
  {
    id: "init-2",
    title: "Community Census & Support",
    slug: "community-census-support",
    description: "Map community needs through surveys and provide targeted support.",
    status: "active",
    category: "community",
    is_online: false,
    is_micro: false,
    created_by: "user-3",
    volunteer_target: 100,
    priority_score: 72,
    start_date: "2026-02-01",
    end_date: "2026-05-31",
    created_at: ago(30240),
    region_name: "Mumbai",
  },
  {
    id: "init-3",
    title: "Urban Reforestation Drive",
    slug: "urban-reforestation-drive",
    description: "Plant 1,000 trees across the city to improve air quality and green cover.",
    status: "active",
    category: "environment",
    is_online: false,
    is_micro: false,
    created_by: "user-5",
    volunteer_target: 150,
    priority_score: 90,
    start_date: "2026-01-01",
    end_date: "2026-12-31",
    created_at: ago(60480),
    region_name: "Mumbai Metro",
  },
];

// ─── Initiative Progress ───
export const mockInitiativeProgress: Record<string, any> = {
  "init-1": {
    initiative_id: "init-1",
    tasks_total: 45,
    tasks_open: 20,
    tasks_in_progress: 12,
    tasks_completed: 13,
    completion_pct: 29,
    contributors_active: 34,
    validators_active: 5,
  },
  "init-2": {
    initiative_id: "init-2",
    tasks_total: 30,
    tasks_open: 8,
    tasks_in_progress: 7,
    tasks_completed: 15,
    completion_pct: 50,
    contributors_active: 22,
    validators_active: 3,
  },
  "init-3": {
    initiative_id: "init-3",
    tasks_total: 60,
    tasks_open: 15,
    tasks_in_progress: 20,
    tasks_completed: 25,
    completion_pct: 42,
    contributors_active: 48,
    validators_active: 8,
  },
};

// ─── Team Members ───
export const mockTeamMembers: Record<string, any> = {
  "demo-1": {
    members: [
      { id: "tm-1", user_id: "user-2", joined_at: ago(1440), profile: mockProfiles[1] },
      { id: "tm-2", user_id: "user-4", joined_at: ago(720), profile: mockProfiles[3] },
      { id: "tm-3", user_id: "user-5", joined_at: ago(360), profile: mockProfiles[4] },
    ],
    member_count: 3,
    team_target: 5,
  },
  "demo-3": {
    members: [
      { id: "tm-4", user_id: "user-1", joined_at: ago(200), profile: mockProfiles[0] },
    ],
    member_count: 1,
    team_target: 3,
  },
  "demo-6": {
    members: [
      { id: "tm-5", user_id: "user-1", joined_at: ago(2880), profile: mockProfiles[0] },
      { id: "tm-6", user_id: "user-2", joined_at: ago(2400), profile: mockProfiles[1] },
      { id: "tm-7", user_id: "user-3", joined_at: ago(1920), profile: mockProfiles[2] },
      { id: "tm-8", user_id: "user-4", joined_at: ago(1200), profile: mockProfiles[3] },
      { id: "tm-9", user_id: "user-5", joined_at: ago(600), profile: mockProfiles[4] },
    ],
    member_count: 5,
    team_target: 8,
  },
  "demo-7": {
    members: [
      { id: "tm-10", user_id: "user-2", joined_at: ago(4320), profile: mockProfiles[1] },
      { id: "tm-11", user_id: "user-3", joined_at: ago(3600), profile: mockProfiles[2] },
    ],
    member_count: 2,
    team_target: 10,
  },
};

// ─── Task Submissions ───
export const mockSubmissions = [
  {
    id: "sub-1",
    task_id: "demo-1",
    user_id: "user-2",
    content: "Completed garden cleanup. Removed debris from 3 plots and planted 15 seedlings. Watered all beds and organized tools in the shed.",
    submitted_at: ago(60),
    status: "pending",
    attachments: [
      { type: "image", url: "/placeholder.svg", name: "before_cleanup.jpg" },
      { type: "image", url: "/placeholder.svg", name: "after_cleanup.jpg" },
    ],
    contributor: mockProfiles[1],
    tasks: mockTasks[0],
  },
  {
    id: "sub-2",
    task_id: "demo-2",
    user_id: "user-3",
    content: "Surveyed 48 businesses on Main Street. Data entered into shared spreadsheet with contact details and operating hours.",
    submitted_at: ago(120),
    status: "pending",
    attachments: [{ type: "document", url: "/placeholder.svg", name: "survey_data.xlsx" }],
    contributor: mockProfiles[2],
    tasks: mockTasks[1],
  },
  {
    id: "sub-3",
    task_id: "demo-4",
    user_id: "user-4",
    content: "Collected 3 water samples from river stations A, B, and C. Lab results pending — samples delivered to lab.",
    submitted_at: ago(240),
    status: "pending",
    attachments: [
      { type: "image", url: "/placeholder.svg", name: "station_a_sample.jpg" },
      { type: "image", url: "/placeholder.svg", name: "station_c_sample.jpg" },
    ],
    contributor: mockProfiles[3],
    tasks: mockTasks[3],
  },
  {
    id: "sub-4",
    task_id: "demo-7",
    user_id: "user-2",
    content: "Painted the first 20 metres of the boundary wall with the base coat. Ready for the mural team tomorrow.",
    submitted_at: ago(30),
    status: "pending",
    attachments: [{ type: "image", url: "/placeholder.svg", name: "wall_progress.jpg" }],
    contributor: mockProfiles[1],
    tasks: mockTasks[6],
  },
];

// ─── Task Updates / Progress ───
export const mockTaskUpdates: Record<string, any[]> = {
  "demo-1": [
    { id: "u-1", task_id: "demo-1", user_id: "user-2", notes: "Started clearing plot A. Lots of overgrowth.", created_at: ago(120) },
    { id: "u-2", task_id: "demo-1", user_id: "user-2", notes: "Plot A done. Moving to B and C now.", created_at: ago(90) },
    { id: "u-3", task_id: "demo-1", user_id: "user-4", notes: "Watering completed for sections A–D.", created_at: ago(60) },
  ],
  "demo-7": [
    { id: "u-4", task_id: "demo-7", user_id: "user-3", notes: "Sketched mural design on paper. Getting approval from school principal.", created_at: ago(200) },
    { id: "u-5", task_id: "demo-7", user_id: "user-2", notes: "Principal approved design. Starting base coat.", created_at: ago(100) },
  ],
};

// ─── Recommendations Feed ───
export const mockRecommendationFeed = {
  recommended: [
    { ...mockTasks[0], _score: 92, _distance: 1.2, _reasons: ["nearby", "interest_match"] },
    { ...mockTasks[1], _score: 85, _distance: 0.8, _reasons: ["nearby", "difficulty_match"] },
    { ...mockTasks[2], _score: 78, _distance: 3.5, _reasons: ["priority", "interest_match"] },
    { ...mockTasks[7], _score: 65, _distance: null, _reasons: ["online", "easy_start"] },
  ],
  nearby: [
    { ...mockTasks[0], _score: 92, _distance: 1.2, _reasons: ["nearby"] },
    { ...mockTasks[3], _score: 70, _distance: 5.1, _reasons: ["medium_distance"] },
    { ...mockTasks[5], _score: 68, _distance: 2.3, _reasons: ["nearby"] },
    { ...mockTasks[6], _score: 60, _distance: 4.0, _reasons: ["nearby"] },
  ],
  priority: [
    { ...mockTasks[2], _score: 78, _distance: 3.5, _reasons: ["priority"] },
    { ...mockTasks[5], _score: 68, _distance: 2.3, _reasons: ["priority"] },
  ],
  online: [
    { ...mockTasks[4], _score: 55, _distance: null, _reasons: ["online"] },
    { ...mockTasks[7], _score: 65, _distance: null, _reasons: ["online"] },
  ],
};

// ─── Impact / Contributions ───
export const mockMyImpact = {
  tasks_completed: 24,
  total_score: 1240,
  initiatives_participated: 5,
  impact_metrics: {
    households_reached: 156,
    trees_planted: 42,
    waste_removed_kg: 89,
    people_served: 210,
  },
  contributions: [
    { id: "c-1", task_id: "demo-1", score: 50, created_at: ago(1440), impact_metrics: { waste_removed_kg: 12 } },
    { id: "c-2", task_id: "demo-2", score: 75, created_at: ago(2880), impact_metrics: { households_reached: 48 } },
    { id: "c-3", task_id: "demo-4", score: 60, created_at: ago(4320), impact_metrics: { waste_removed_kg: 8 } },
  ],
};

// ─── Community Feed ───
export const mockCommunityFeed = {
  feed: [
    { id: "cf-1", type: "task_completed", actor: mockProfiles[1], task: mockTasks[0], created_at: ago(30), message: "completed Community Garden Cleanup" },
    { id: "cf-2", type: "initiative_joined", actor: mockProfiles[3], initiative: mockInitiatives[0], created_at: ago(60), message: "joined River Cleanup Campaign" },
    { id: "cf-3", type: "badge_earned", actor: mockProfiles[0], badge: { name: "Environment Hero", level: "gold" }, created_at: ago(120), message: "earned the Environment Hero badge" },
    { id: "cf-4", type: "task_completed", actor: mockProfiles[2], task: mockTasks[1], created_at: ago(180), message: "completed Survey Local Businesses" },
    { id: "cf-5", type: "milestone_reached", initiative: mockInitiatives[2], milestone: { percentage: 75 }, created_at: ago(240), message: "Urban Reforestation Drive reached 75%" },
    { id: "cf-6", type: "task_completed", actor: mockProfiles[4], task: mockTasks[6], created_at: ago(360), message: "completed Paint School Boundary Wall" },
  ],
};

// ─── Validations (pending) ───
export const mockPendingValidations = {
  submissions: mockSubmissions.map((s) => ({
    ...s,
    contributor: { display_name: s.contributor.display_name, avatar_url: s.contributor.avatar_url },
    tasks: { id: s.tasks.id, title: s.tasks.title, org_id: s.tasks.org_id, difficulty: s.tasks.difficulty, tags: s.tasks.tags },
  })),
};

// ─── Credentials ───
export const mockCredentials = [
  { id: "cred-1", title: "Community Outreach – Gold", type: "badge", user_id: "user-1", is_active: true, issued_at: ago(10080), description: "Awarded for outstanding community outreach" },
  { id: "cred-2", title: "Environmental Steward – Silver", type: "badge", user_id: "user-1", is_active: true, issued_at: ago(20160), description: "Recognized for environmental contributions" },
  { id: "cred-3", title: "River Cleanup Completion", type: "completion", user_id: "user-1", is_active: true, issued_at: ago(5040), description: "Completed all River Cleanup Campaign tasks" },
];

// ─── Leadership / Roles ───
export const mockLeadershipData = {
  roles: [
    { user_id: "user-1", role: "admin" },
    { user_id: "user-3", role: "moderator" },
    { user_id: "user-5", role: "validator" },
  ],
  orgMembers: [
    { user_id: "user-1", org_id: "org-1", role: "admin", profile: mockProfiles[0] },
    { user_id: "user-2", org_id: "org-1", role: "member", profile: mockProfiles[1] },
    { user_id: "user-3", org_id: "org-1", role: "moderator", profile: mockProfiles[2] },
    { user_id: "user-4", org_id: "org-2", role: "member", profile: mockProfiles[3] },
    { user_id: "user-5", org_id: "org-2", role: "admin", profile: mockProfiles[4] },
  ],
};

// ─── Notifications ───
export const mockNotifications = [
  { id: "notif-1", title: "Task Approved", body: "Your submission for Community Garden Cleanup was approved!", type: "validation", read: false, created_at: ago(15), data: { task_id: "demo-1" } },
  { id: "notif-2", title: "New Team Member", body: "Priya Sharma joined your team for Youth Coding Workshop", type: "team", read: false, created_at: ago(60), data: { task_id: "demo-3" } },
  { id: "notif-3", title: "Initiative Milestone", body: "Urban Reforestation Drive reached 75% completion!", type: "milestone", read: true, created_at: ago(240), data: { initiative_id: "init-3" } },
  { id: "notif-4", title: "Badge Earned", body: "You earned the Community Outreach Gold badge!", type: "badge", read: true, created_at: ago(1440), data: {} },
];

// ─── User Interests ───
export const mockUserInterests = {
  interests: ["environment", "community", "education", "outdoor"],
};

// ─── Next Mission ───
export const mockNextMission = {
  next_mission: {
    id: "demo-3",
    title: "Youth Coding Workshop Helper",
    estimatedTime: "3h",
    distance: "3.5 km",
    difficulty: 4,
    tags: ["community", "education"],
  },
};

// ─── Impact per task (contextualized) ───
const taskImpactMap: Record<string, string> = {
  "demo-1": "🌿 15 seedlings planted, 3 plots cleared",
  "demo-2": "📊 48 businesses surveyed & documented",
  "demo-3": "🎓 20 students mentored in coding",
  "demo-4": "🔬 3 water quality samples collected",
  "demo-5": "🏥 Health flyers translated to 2 languages",
  "demo-6": "🍲 200+ food items sorted & packed",
  "demo-7": "🎨 20m boundary wall painted",
  "demo-8": "📋 500 census records digitized",
};

const taskEstTimeMap: Record<string, string> = {
  "demo-1": "1.5h",
  "demo-2": "3h",
  "demo-3": "4h",
  "demo-4": "2h",
  "demo-5": "45min",
  "demo-6": "2.5h",
  "demo-7": "5h",
  "demo-8": "1h",
};

const taskDistanceMap: Record<string, string | null> = {
  "demo-1": "1.2 km",
  "demo-2": "0.8 km",
  "demo-3": "3.5 km",
  "demo-4": "5.1 km",
  "demo-5": null,
  "demo-6": "2.3 km",
  "demo-7": "4.0 km",
  "demo-8": null,
};

// ─── Lookup helpers ───
export function getMockTask(id: string) {
  return mockTasks.find((t) => t.id === id || t.slug === id) ?? null;
}

export function getMockOrg(orgId: string | null) {
  return mockOrganizations.find((o) => o.id === orgId) ?? null;
}

export function getMockInitiative(initId: string | null) {
  return mockInitiatives.find((i) => i.id === initId) ?? null;
}

export function getMockTaskImpact(taskId: string) {
  return taskImpactMap[taskId] ?? "🌱 Positive community impact";
}

export function getMockEstTime(taskId: string) {
  return taskEstTimeMap[taskId] ?? "1h";
}

export function getMockDistance(taskId: string) {
  return taskDistanceMap[taskId];
}

export function getMockTeam(taskId: string) {
  return mockTeamMembers[taskId] ?? { members: [], member_count: 0, team_target: 5 };
}

export function getMockSubmissions(taskId: string) {
  return mockSubmissions.filter((s) => s.task_id === taskId);
}

export function getMockTaskUpdates(taskId: string) {
  return mockTaskUpdates[taskId] ?? [];
}
