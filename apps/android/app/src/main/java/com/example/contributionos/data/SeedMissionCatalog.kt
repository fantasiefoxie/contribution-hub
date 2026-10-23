package com.example.contributionos.data

import com.example.contributionos.model.CivicBadge
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.CommunityMilestone
import com.example.contributionos.model.ExpeditionNode
import com.example.contributionos.model.ExpeditionStatus
import com.example.contributionos.model.MapWaypoint
import com.example.contributionos.model.NodeStatus
import kotlin.random.Random

object SeedMissionCatalog {

  val testWaypoints = listOf(
    MapWaypoint(
      id = "wp-canopy-banyan-1",
      title = "[TEST] Heritage Banyan Canopy Checkpoint",
      description = "Prominent mature shade canopy landmark with 15m radius spread. Ideal reference point for pedestrian walkability and canopy shading audits.",
      category = "Tree Canopy",
      latitude = 12.9716,
      longitude = 77.5946,
      xpReward = 50,
      linkedTaskId = "self-guided-groundwork-1",
      isSandboxTest = true
    ),
    MapWaypoint(
      id = "wp-water-bioswale-2",
      title = "[TEST] Stormwater Catchment Bioswale",
      description = "Municipal rainwater retention and percolation swale. Check for seasonal sediment buildup, plastic blockage, or curb inlet damage.",
      category = "Waterway Basin",
      latitude = 12.9745,
      longitude = 77.5980,
      xpReward = 50,
      linkedTaskId = "self-guided-bioswale-2",
      isSandboxTest = true
    ),
    MapWaypoint(
      id = "wp-solar-transit-3",
      title = "[TEST] Public Transit Solar Hub",
      description = "Rooftop solar shelter providing renewable shade and device charging along 4th Main corridor.",
      category = "Clean Energy",
      latitude = 12.9680,
      longitude = 77.5912,
      xpReward = 50,
      linkedTaskId = "self-guided-solar-3",
      isSandboxTest = true
    ),
    MapWaypoint(
      id = "wp-footpath-crossing-4",
      title = "[TEST] Pedestrian Tactile Crossing",
      description = "Universal accessibility ramp and tactile paving station. Verify crosswalk visibility and pavement integrity.",
      category = "Accessibility",
      latitude = 12.9760,
      longitude = 77.5925,
      xpReward = 50,
      linkedTaskId = "self-guided-tactile-4",
      isSandboxTest = true
    )
  )

  val selfGuidedStarterQuest = CivicTask(
    id = "self-guided-groundwork-1",
    title = "[SELF-GUIDED] Neighborhood Walkability & Canopy Scout",
    description = """Step outside for a self-paced walk around your immediate neighborhood.

COMPLETION RULES (all three required):
① Walk for at least 10–15 minutes on public footpaths or sidewalks.
② Observe and mentally note (or write down) at least 2 of the following: tree shade spots, footpath obstacles, unlit areas, or public spaces in poor repair.
③ Return safely and write a short field note (1–3 sentences) describing what you saw.

STATIONARY / REMOTE ALTERNATIVE — if mobility, health, or safety prevents going outside:
Observe from a window, balcony, or doorway for at least 10 minutes. Describe what you see on the street or in nearby outdoor space. This counts as a full completion.

This is a self-guided exploration activity. It completes locally on your device. It is NOT a verified civic contribution and does NOT require authentication, location, or camera access.""",
    difficulty = 1,
    status = "open",
    tags = listOf("self-guided", "walkability", "canopy", "anti-grind"),
    district = "Local Neighborhood Biome",
    locationLabel = "Immediate Vicinity",
    evidenceRequirements = "Required: A short field note (1–3 sentences) describing your observations. Optional: 1 photo of a pathway, tree, or public space. Both are stored only on your device and never uploaded.",
    estimatedDurationMinutes = 15,
    safetyNotice = "Stay alert near moving traffic, respect private property boundaries, stay on public sidewalks, and hydrate. Do not attempt at night unless your route is well-lit. Use the stationary alternative if going outside is not safe or accessible.",
    isSandboxDemo = false,
    isLocalCompletionOnly = true
  )

  val selfGuidedBioswaleQuest = CivicTask(
    id = "self-guided-bioswale-2",
    title = "[SELF-GUIDED] Stormwater Catchment & Bioswale Scout",
    description = """Scout nearby street drainage lines, curb cutouts, or roadside swales.

COMPLETION RULES (all three required):
① Walk or inspect a local curb, drain, or vegetation strip for 10–15 minutes.
② Observe whether rainwater flows freely or if debris, silt, or plastics obstruct runoff channels.
③ Record a short field note (1–3 sentences) describing your observations.

STATIONARY / REMOTE ALTERNATIVE:
Observe street drainage, rain gutters, or surface runoff patterns from your residence window or porch. Describe water drainage conditions. This counts as a full completion.

This is a self-guided exploration activity. It completes locally on your device. It is NOT a verified civic contribution and does NOT require authentication, location, or camera access.""",
    difficulty = 1,
    status = "open",
    tags = listOf("self-guided", "water", "drainage", "anti-grind"),
    district = "Local Neighborhood Biome",
    locationLabel = "Immediate Vicinity",
    evidenceRequirements = "Required: A short field note (1–3 sentences) describing drainage conditions. Optional: 1 photo. Stored only on your device.",
    estimatedDurationMinutes = 15,
    safetyNotice = "Stay clear of fast-flowing stormwater drains during active downpours. Stay on sidewalks. Use the stationary alternative if weather or mobility warrants.",
    isSandboxDemo = false,
    isLocalCompletionOnly = true
  )

  val selfGuidedSolarQuest = CivicTask(
    id = "self-guided-solar-3",
    title = "[SELF-GUIDED] Shaded Transit Corridor & Clean Energy Audit",
    description = """Survey shaded transit shelters, solar rooftop canopies, or public benches in your neighborhood.

COMPLETION RULES (all three required):
① Scout nearby transit shelters or public seating areas for 10–15 minutes.
② Note availability of shade, shelter from elements, and general maintenance status.
③ Record a short field note (1–3 sentences) describing your observations.

STATIONARY / REMOTE ALTERNATIVE:
Observe nearby bus stops, street corners, or rooftop solar panels from a window or balcony. Note shade coverage and pedestrian activity. Counts as full completion.

This is a self-guided exploration activity. It completes locally on your device. It is NOT a verified civic contribution and does NOT require authentication, location, or camera access.""",
    difficulty = 1,
    status = "open",
    tags = listOf("self-guided", "transit", "energy", "anti-grind"),
    district = "Local Neighborhood Biome",
    locationLabel = "Immediate Vicinity",
    evidenceRequirements = "Required: A short field note (1–3 sentences) on transit shade and seating. Optional: 1 photo.",
    estimatedDurationMinutes = 15,
    safetyNotice = "Do not obstruct commuters or transit boarding areas. Respect private boundaries. Stay hydrated.",
    isSandboxDemo = false,
    isLocalCompletionOnly = true
  )

  val selfGuidedTactileQuest = CivicTask(
    id = "self-guided-tactile-4",
    title = "[SELF-GUIDED] Pedestrian Accessibility & Ramp Audit",
    description = """Audit universal accessibility, tactile sidewalk paving, and curb ramp transitions.

COMPLETION RULES (all three required):
① Walk or roll along a local public walkway for 10–15 minutes.
② Check for curb ramps, level footpath transitions, tactile tiles, or step obstacles.
③ Record a short field note (1–3 sentences) summarizing step-free access quality.

STATIONARY / REMOTE ALTERNATIVE:
Inspect doorstep, entryway ramp, or residential corridor thresholds for step-free barrier clearance. Describe observations. Counts as full completion.

This is a self-guided exploration activity. It completes locally on your device. It is NOT a verified civic contribution and does NOT require authentication, location, or camera access.""",
    difficulty = 1,
    status = "open",
    tags = listOf("self-guided", "accessibility", "walkability", "anti-grind"),
    district = "Local Neighborhood Biome",
    locationLabel = "Immediate Vicinity",
    evidenceRequirements = "Required: A short field note (1–3 sentences) on ramp and pavement accessibility. Optional: 1 photo.",
    estimatedDurationMinutes = 15,
    safetyNotice = "Stay alert near curb crossings and driveway intersections. Do not step into active motor lanes.",
    isSandboxDemo = false,
    isLocalCompletionOnly = true
  )

  val selfGuidedQuests = listOf(
    selfGuidedStarterQuest,
    selfGuidedBioswaleQuest,
    selfGuidedSolarQuest,
    selfGuidedTactileQuest
  )

  val selfGuidedQuest: CivicTask get() = selfGuidedStarterQuest

  fun findTask(taskId: String): CivicTask? {
    return selfGuidedQuests.find { it.id == taskId }
      ?: realTasks.find { it.id == taskId }
  }

  val realTasks = listOf(
    CivicTask(
      id = "demo-task-green-garden-1",
      title = "[DEMO] Community Garden Canopy Restoration",
      description = "Weed invasive vines, mulch tree beds, and install rainwater catchment guards along the south perimeter.",
      difficulty = 2,
      status = "open",
      tags = listOf("environment", "forestry", "outdoor"),
      district = "Greenway District",
      locationLabel = "South Basin Commons (Sandbox)",
      evidenceRequirements = "Submit 1 photo of mulched bed and 1 geotagged note confirming perimeter inspection.",
      isSandboxDemo = true
    ),
    CivicTask(
      id = "demo-task-river-testing-2",
      title = "[DEMO] River Basin Turbidity & pH Analysis",
      description = "Collect 3 water samples at designated waypoint markers along the creek and record test-strip readings.",
      difficulty = 3,
      status = "open",
      tags = listOf("water", "research", "ecology"),
      district = "Harbor Basin",
      locationLabel = "Dock 4 Waterway Station (Sandbox)",
      evidenceRequirements = "Photograph color chart next to test strip and log GPS coordinates.",
      isSandboxDemo = true
    ),
    CivicTask(
      id = "demo-task-business-survey-3",
      title = "[DEMO] Local Merchant Accessibility Audit",
      description = "Survey 4 public storefronts along the transit corridor for step-free access, door widths, and tactile walkways.",
      difficulty = 3,
      status = "open",
      tags = listOf("accessibility", "community", "urban-planning"),
      district = "Civic Core",
      locationLabel = "Transit Central Arcade (Sandbox)",
      evidenceRequirements = "Document ramp slope or step height and record entrance photos.",
      isSandboxDemo = true
    ),
    CivicTask(
      id = "demo-task-tree-inventory-4",
      title = "[DEMO] Old-Growth Canopy Diameter Census",
      description = "Measure trunk circumference and document canopy health indicators for mature native trees in the preserve.",
      difficulty = 4,
      status = "open",
      tags = listOf("forestry", "research", "conservation"),
      district = "Old Town Canopy",
      locationLabel = "Ancient Oak Sanctuary (Sandbox)",
      evidenceRequirements = "Measurement tape reading photo and tree health notes.",
      isSandboxDemo = true
    ),
    CivicTask(
      id = "demo-task-food-sort-5",
      title = "[DEMO] Mutual Aid Nutrition Depot Sorting",
      description = "Sort, inventory, and crate fresh regional produce for distribution to low-income senior households.",
      difficulty = 2,
      status = "open",
      tags = listOf("food-security", "logistics", "community"),
      district = "Civic Core",
      locationLabel = "Civic Pantry Depot (Sandbox)",
      evidenceRequirements = "Count of crates packed and signed dispatch log.",
      isSandboxDemo = true
    ),
    CivicTask(
      id = "demo-task-creek-debris-6",
      title = "[DEMO] Riparian Habitat Debris Removal",
      description = "Extract plastics and industrial litter from the tidal marsh buffer zone before high-tide dispersion.",
      difficulty = 4,
      status = "open",
      tags = listOf("cleanup", "marshland", "wildlife"),
      district = "Harbor Basin",
      locationLabel = "Tidal Saltmarsh Boundary (Sandbox)",
      evidenceRequirements = "Before/after site comparison photos and estimated weight haul.",
      isSandboxDemo = true
    ),
    CivicTask(
      id = "demo-task-solar-audit-7",
      title = "[DEMO] Municipal Solar Canopy Shade Survey",
      description = "Audit rooftop micro-inverters and identify excessive shade obstructions from overhanging branches.",
      difficulty = 3,
      status = "open",
      tags = listOf("energy", "infrastructure", "audit"),
      district = "Greenway District",
      locationLabel = "Solar Array Substation B (Sandbox)",
      evidenceRequirements = "Photo of azimuth compass reading and shade line notation.",
      isSandboxDemo = true
    )
  )

  val realMilestones = listOf(
    CommunityMilestone(
      id = "ms-demo-1",
      title = "[DEMO] Urban Canopy Reforestation",
      description = "Simulated civic goal to plant and catalog native trees across all districts.",
      currentValue = 1840,
      targetValue = 2500,
      unit = "trees mapped",
      category = "Environment",
      isSandboxDemo = true
    ),
    CommunityMilestone(
      id = "ms-demo-2",
      title = "[DEMO] Riparian Basin Litter Cleared",
      description = "Simulated removal of non-biodegradable plastics from waterways before entering the bay.",
      currentValue = 9420,
      targetValue = 12000,
      unit = "kg litter removed",
      category = "Ecology",
      isSandboxDemo = true
    ),
    CommunityMilestone(
      id = "ms-demo-3",
      title = "[DEMO] Universal Access Mapping",
      description = "Simulated audits of crosswalks and commercial entrances for wheelchair access.",
      currentValue = 480,
      targetValue = 600,
      unit = "audits verified",
      category = "Accessibility",
      isSandboxDemo = true
    )
  )

  val defaultProfile = CivicProfile(
    id = "user-demo-1",
    displayName = "Touch Grass Explorer (Demo)",
    callsign = "Grasshopper-01",
    level = 4,
    xp = 1420,
    nextLevelXp = 2000,
    rankTitle = "Actually Useful Human [Sandbox]",
    verifiedContributionsCount = 18,
    completedExpeditionsCount = 5,
    activeStreakDays = 6,
    specialization = "Urban Ecology & Canopy Stewardship",
    badges = listOf(
      CivicBadge("b-1", "First Groundwork", "Touched real grass for the first time.", "🌱", true),
      CivicBadge("b-2", "Canopy Steward", "Mapped 10+ urban shade trees.", "🌳", true),
      CivicBadge("b-3", "River Watcher", "Sampled watershed quality at 5 stations.", "🌊", true),
      CivicBadge("b-4", "Night Vigil", "Conducted evening pedestrian safety audit.", "🔦", false),
      CivicBadge("b-5", "Master Cartographer", "Cleared an entire 4-tier expedition run.", "🧭", false)
    ),
    isSandboxDemo = true
  )

  fun generateExpeditions(seed: Long = 42L): List<CivicExpedition> {
    val rng = Random(seed)

    val exp1Nodes = listOf(
      ExpeditionNode(
        id = "demo-node-1-0",
        taskId = "demo-task-green-garden-1",
        step = 0,
        branchIndex = 0,
        title = "South Basin Groundwork",
        category = "Ecology",
        difficulty = 2,
        xpReward = 80,
        status = NodeStatus.VALIDATED,
        isoX = 0.25f,
        isoY = 0.70f,
        childNodeIds = listOf("demo-node-1-1A", "demo-node-1-1B"),
        isSandboxDemo = true
      ),
      ExpeditionNode(
        id = "demo-node-1-1A",
        taskId = "demo-task-solar-audit-7",
        step = 1,
        branchIndex = 0,
        title = "Solar Overlook Audit",
        category = "Energy",
        difficulty = 3,
        xpReward = 120,
        status = NodeStatus.AVAILABLE,
        isoX = 0.40f,
        isoY = 0.45f,
        childNodeIds = listOf("demo-node-1-2A"),
        isSandboxDemo = true
      ),
      ExpeditionNode(
        id = "demo-node-1-1B",
        taskId = "demo-task-river-testing-2",
        step = 1,
        branchIndex = 1,
        title = "Harbor Runoff Testing",
        category = "Waterway",
        difficulty = 3,
        xpReward = 130,
        status = NodeStatus.AVAILABLE,
        isoX = 0.55f,
        isoY = 0.58f,
        childNodeIds = listOf("demo-node-1-2A", "demo-node-1-2B"),
        isSandboxDemo = true
      ),
      ExpeditionNode(
        id = "demo-node-1-2A",
        taskId = "demo-task-business-survey-3",
        step = 2,
        branchIndex = 0,
        title = "Arcade Transit Passage",
        category = "Accessibility",
        difficulty = 3,
        xpReward = 150,
        status = NodeStatus.LOCKED,
        isoX = 0.65f,
        isoY = 0.35f,
        childNodeIds = listOf("demo-node-1-3"),
        isSandboxDemo = true
      ),
      ExpeditionNode(
        id = "demo-node-1-2B",
        taskId = "demo-task-food-sort-5",
        step = 2,
        branchIndex = 1,
        title = "Pantry Distribution Station",
        category = "Mutual Aid",
        difficulty = 2,
        xpReward = 110,
        status = NodeStatus.LOCKED,
        isoX = 0.75f,
        isoY = 0.50f,
        childNodeIds = listOf("demo-node-1-3"),
        isSandboxDemo = true
      ),
      ExpeditionNode(
        id = "demo-node-1-3",
        taskId = "demo-task-tree-inventory-4",
        step = 3,
        branchIndex = 0,
        title = "Crown Canopy Apex",
        category = "Reforestation",
        difficulty = 4,
        xpReward = 250,
        status = NodeStatus.LOCKED,
        isoX = 0.85f,
        isoY = 0.22f,
        childNodeIds = emptyList(),
        isSandboxDemo = true
      )
    )

    val exp1 = CivicExpedition(
      id = "demo-expedition-emerald-canopy",
      title = "[DEMO] Verdant Canopy Expedition",
      theme = "Urban Forestry & Riparian Recovery",
      lore = "[SANDBOX SIMULATION] A branching tactical route spanning the Greenway Corridor to the Old Town heights. Demonstrates roguelite node progression with fixture data.",
      district = "Greenway District (Sandbox)",
      targetMilestone = "[DEMO] Urban Canopy Reforestation",
      nodes = exp1Nodes,
      activeRunSeed = seed,
      status = ExpeditionStatus.ACTIVE,
      isSandboxDemo = true
    )

    val exp2Nodes = listOf(
      ExpeditionNode(
        id = "demo-node-2-0",
        taskId = "demo-task-creek-debris-6",
        step = 0,
        branchIndex = 0,
        title = "Saltmarsh Entry Scrub",
        category = "Cleanup",
        difficulty = 4,
        xpReward = 160,
        status = NodeStatus.AVAILABLE,
        isoX = 0.30f,
        isoY = 0.65f,
        childNodeIds = listOf("demo-node-2-1A"),
        isSandboxDemo = true
      ),
      ExpeditionNode(
        id = "demo-node-2-1A",
        taskId = "demo-task-river-testing-2",
        step = 1,
        branchIndex = 0,
        title = "Dockside Water Monitoring",
        category = "Waterway",
        difficulty = 3,
        xpReward = 140,
        status = NodeStatus.LOCKED,
        isoX = 0.60f,
        isoY = 0.40f,
        childNodeIds = emptyList(),
        isSandboxDemo = true
      )
    )

    val exp2 = CivicExpedition(
      id = "demo-expedition-harbor-cleanse",
      title = "[DEMO] Harbor Basin Wayfinding",
      theme = "Tidal Protection & Marine Ecology",
      lore = "[SANDBOX SIMULATION] Track runoff flows, eliminate hazardous plastics from estuaries, and log watershed indicators in sandbox mode.",
      district = "Harbor Basin (Sandbox)",
      targetMilestone = "[DEMO] Riparian Basin Litter Cleared",
      nodes = exp2Nodes,
      activeRunSeed = seed + 100,
      status = ExpeditionStatus.ACTIVE,
      isSandboxDemo = true
    )

    return listOf(exp1, exp2)
  }
}
