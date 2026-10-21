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
      id = "task-green-garden-1",
      title = "Community Garden Canopy Restoration",
      description = "Weed invasive vines, mulch tree beds, and install rainwater catchment guards along the south perimeter.",
      difficulty = 2,
      status = "open",
      tags = listOf("environment", "forestry", "outdoor"),
      district = "Greenway District",
      locationLabel = "South Basin Commons",
      evidenceRequirements = "Submit 1 photo of mulched bed and 1 geotagged note confirming perimeter inspection."
    ),
    CivicTask(
      id = "task-river-testing-2",
      title = "River Basin Turbidity & pH Analysis",
      description = "Collect 3 water samples at designated waypoint markers along the creek and record test-strip readings.",
      difficulty = 3,
      status = "open",
      tags = listOf("water", "research", "ecology"),
      district = "Harbor Basin",
      locationLabel = "Dock 4 Waterway Station",
      evidenceRequirements = "Photograph color chart next to test strip and log GPS coordinates."
    ),
    CivicTask(
      id = "task-business-survey-3",
      title = "Local Merchant Accessibility Audit",
      description = "Survey 4 public storefronts along the transit corridor for step-free access, door widths, and tactile walkways.",
      difficulty = 3,
      status = "open",
      tags = listOf("accessibility", "community", "urban-planning"),
      district = "Civic Core",
      locationLabel = "Transit Central Arcade",
      evidenceRequirements = "Document ramp slope or step height and record entrance photos."
    ),
    CivicTask(
      id = "task-tree-inventory-4",
      title = "Old-Growth Canopy Diameter Census",
      description = "Measure trunk circumference and document canopy health indicators for mature native trees in the preserve.",
      difficulty = 4,
      status = "open",
      tags = listOf("forestry", "research", "conservation"),
      district = "Old Town Canopy",
      locationLabel = "Ancient Oak Sanctuary",
      evidenceRequirements = "Measurement tape reading photo and tree health notes."
    ),
    CivicTask(
      id = "task-food-sort-5",
      title = "Mutual Aid Nutrition Depot Sorting",
      description = "Sort, inventory, and crate fresh regional produce for distribution to low-income senior households.",
      difficulty = 2,
      status = "open",
      tags = listOf("food-security", "logistics", "community"),
      district = "Civic Core",
      locationLabel = "Civic Pantry Depot",
      evidenceRequirements = "Count of crates packed and signed dispatch log."
    ),
    CivicTask(
      id = "task-creek-debris-6",
      title = "Riparian Habitat Debris Removal",
      description = "Extract plastics and industrial litter from the tidal marsh buffer zone before high-tide dispersion.",
      difficulty = 4,
      status = "open",
      tags = listOf("cleanup", "marshland", "wildlife"),
      district = "Harbor Basin",
      locationLabel = "Tidal Saltmarsh Boundary",
      evidenceRequirements = "Before/after site comparison photos and estimated weight haul."
    ),
    CivicTask(
      id = "task-solar-audit-7",
      title = "Municipal Solar Canopy Shade Survey",
      description = "Audit rooftop micro-inverters and identify excessive shade obstructions from overhanging branches.",
      difficulty = 3,
      status = "open",
      tags = listOf("energy", "infrastructure", "audit"),
      district = "Greenway District",
      locationLabel = "Solar Array Substation B",
      evidenceRequirements = "Photo of azimuth compass reading and shade line notation."
    )
  )

  val realMilestones = listOf(
    CommunityMilestone(
      id = "ms-1",
      title = "Urban Canopy Reforestation",
      description = "Collective community goal to plant and catalog native trees across all districts.",
      currentValue = 1840,
      targetValue = 2500,
      unit = "trees mapped",
      category = "Environment"
    ),
    CommunityMilestone(
      id = "ms-2",
      title = "Riparian Basin Litter Cleared",
      description = "Removing non-biodegradable plastics from waterways before entering the ocean bay.",
      currentValue = 9420,
      targetValue = 12000,
      unit = "kg litter removed",
      category = "Ecology"
    ),
    CommunityMilestone(
      id = "ms-3",
      title = "Universal Access Mapping",
      description = "Auditing crosswalks, sidewalk cuts, and commercial entrances for wheelchair access.",
      currentValue = 480,
      targetValue = 600,
      unit = "audits verified",
      category = "Accessibility"
    )
  )

  val defaultProfile = CivicProfile(
    id = "user-rogue-1",
    displayName = "Civic Wayfinder",
    callsign = "Verdant-07",
    level = 4,
    xp = 1420,
    nextLevelXp = 2000,
    rankTitle = "Senior Field Surveyor",
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
    )
  )

  fun generateExpeditions(seed: Long = 42L): List<CivicExpedition> {
    val rng = Random(seed)

    val exp1Nodes = listOf(
      ExpeditionNode(
        id = "node-1-0",
        taskId = "task-green-garden-1",
        step = 0,
        branchIndex = 0,
        title = "South Basin Groundwork",
        category = "Ecology",
        difficulty = 2,
        xpReward = 80,
        status = NodeStatus.VALIDATED,
        isoX = 0.25f,
        isoY = 0.70f,
        childNodeIds = listOf("node-1-1A", "node-1-1B")
      ),
      ExpeditionNode(
        id = "node-1-1A",
        taskId = "task-solar-audit-7",
        step = 1,
        branchIndex = 0,
        title = "Solar Overlook Audit",
        category = "Energy",
        difficulty = 3,
        xpReward = 120,
        status = NodeStatus.AVAILABLE,
        isoX = 0.40f,
        isoY = 0.45f,
        childNodeIds = listOf("node-1-2A")
      ),
      ExpeditionNode(
        id = "node-1-1B",
        taskId = "task-river-testing-2",
        step = 1,
        branchIndex = 1,
        title = "Harbor Runoff Testing",
        category = "Waterway",
        difficulty = 3,
        xpReward = 130,
        status = NodeStatus.AVAILABLE,
        isoX = 0.55f,
        isoY = 0.58f,
        childNodeIds = listOf("node-1-2A", "node-1-2B")
      ),
      ExpeditionNode(
        id = "node-1-2A",
        taskId = "task-business-survey-3",
        step = 2,
        branchIndex = 0,
        title = "Arcade Transit Passage",
        category = "Accessibility",
        difficulty = 3,
        xpReward = 150,
        status = NodeStatus.LOCKED,
        isoX = 0.65f,
        isoY = 0.35f,
        childNodeIds = listOf("node-1-3")
      ),
      ExpeditionNode(
        id = "node-1-2B",
        taskId = "task-food-sort-5",
        step = 2,
        branchIndex = 1,
        title = "Pantry Distribution Station",
        category = "Mutual Aid",
        difficulty = 2,
        xpReward = 110,
        status = NodeStatus.LOCKED,
        isoX = 0.75f,
        isoY = 0.50f,
        childNodeIds = listOf("node-1-3")
      ),
      ExpeditionNode(
        id = "node-1-3",
        taskId = "task-tree-inventory-4",
        step = 3,
        branchIndex = 0,
        title = "Crown Canopy Apex",
        category = "Reforestation",
        difficulty = 4,
        xpReward = 250,
        status = NodeStatus.LOCKED,
        isoX = 0.85f,
        isoY = 0.22f,
        childNodeIds = emptyList()
      )
    )

    val exp1 = CivicExpedition(
      id = "expedition-emerald-canopy",
      title = "Verdant Canopy Expedition",
      theme = "Urban Forestry & Riparian Recovery",
      lore = "A branching tactical route spanning the Greenway Corridor to the Old Town heights. Each completed station feeds collective civic records and permanent tree survival data.",
      district = "Greenway District",
      targetMilestone = "Urban Canopy Reforestation",
      nodes = exp1Nodes,
      activeRunSeed = seed,
      status = ExpeditionStatus.ACTIVE
    )

    val exp2Nodes = listOf(
      ExpeditionNode(
        id = "node-2-0",
        taskId = "task-creek-debris-6",
        step = 0,
        branchIndex = 0,
        title = "Saltmarsh Entry Scrub",
        category = "Cleanup",
        difficulty = 4,
        xpReward = 160,
        status = NodeStatus.AVAILABLE,
        isoX = 0.30f,
        isoY = 0.65f,
        childNodeIds = listOf("node-2-1A")
      ),
      ExpeditionNode(
        id = "node-2-1A",
        taskId = "task-river-testing-2",
        step = 1,
        branchIndex = 0,
        title = "Dockside Water Monitoring",
        category = "Waterway",
        difficulty = 3,
        xpReward = 140,
        status = NodeStatus.LOCKED,
        isoX = 0.60f,
        isoY = 0.40f,
        childNodeIds = emptyList()
      )
    )

    val exp2 = CivicExpedition(
      id = "expedition-harbor-cleanse",
      title = "Harbor Basin Wayfinding",
      theme = "Tidal Protection & Marine Ecology",
      lore = "Track runoff flows, eliminate hazardous plastics from riparian estuaries, and log watershed indicators.",
      district = "Harbor Basin",
      targetMilestone = "Riparian Basin Litter Cleared",
      nodes = exp2Nodes,
      activeRunSeed = seed + 100,
      status = ExpeditionStatus.ACTIVE
    )

    return listOf(exp1, exp2)
  }
}
