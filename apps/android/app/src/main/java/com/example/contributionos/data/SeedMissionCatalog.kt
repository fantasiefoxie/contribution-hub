package com.example.contributionos.data

import com.example.contributionos.model.CivicBadge
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.CommunityMilestone
import com.example.contributionos.model.ExpeditionNode
import com.example.contributionos.model.ExpeditionStatus
import com.example.contributionos.model.NodeStatus
import kotlin.random.Random

object SeedMissionCatalog {

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
    displayName = "Civic Wayfinder (Demo)",
    callsign = "Verdant-Demo",
    level = 4,
    xp = 1420,
    nextLevelXp = 2000,
    rankTitle = "Senior Field Surveyor [Sandbox]",
    verifiedContributionsCount = 18,
    completedExpeditionsCount = 5,
    activeStreakDays = 6,
    specialization = "Urban Ecology & Canopy Stewardship",
    badges = listOf(
      CivicBadge("b-1", "First Groundwork", "Completed initial field validation.", "🌱", true),
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
