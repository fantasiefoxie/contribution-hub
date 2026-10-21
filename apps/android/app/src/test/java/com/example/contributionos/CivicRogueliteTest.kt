package com.example.contributionos

import com.example.contributionos.data.SeedMissionCatalog
import com.example.contributionos.model.ExpeditionStatus
import com.example.contributionos.model.MissionDraft
import com.example.contributionos.model.NodeStatus
import com.example.contributionos.model.SubmissionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CivicRogueliteTest {

  @Test
  fun testDeterministicExpeditionGeneration() {
    val expA = SeedMissionCatalog.generateExpeditions(seed = 42L)
    val expB = SeedMissionCatalog.generateExpeditions(seed = 42L)

    assertEquals("Expected identical expedition count", expA.size, expB.size)
    assertEquals("Expected matching expedition title", expA[0].title, expB[0].title)
    assertEquals("Expected matching nodes count", expA[0].nodes.size, expB[0].nodes.size)
    assertEquals("Expected matching seed", expA[0].activeRunSeed, expB[0].activeRunSeed)

    for (i in expA[0].nodes.indices) {
      val nodeA = expA[0].nodes[i]
      val nodeB = expB[0].nodes[i]
      assertEquals("Node ID at $i should match", nodeA.id, nodeB.id)
      assertEquals("Node status at $i should match", nodeA.status, nodeB.status)
      assertEquals("Node isoX at $i should match", nodeA.isoX, nodeB.isoX, 0.001f)
      assertEquals("Node isoY at $i should match", nodeA.isoY, nodeB.isoY, 0.001f)
    }
  }

  @Test
  fun testDifferentSeedsProduceDifferentSeeds() {
    val expA = SeedMissionCatalog.generateExpeditions(seed = 42L)
    val expB = SeedMissionCatalog.generateExpeditions(seed = 99L)

    assertEquals(expA[0].activeRunSeed, 42L)
    assertEquals(expB[0].activeRunSeed, 99L)
  }

  @Test
  fun testRealMissionDataIntegrity() {
    val tasks = SeedMissionCatalog.realTasks
    assertTrue("Real tasks catalog should have items", tasks.isNotEmpty())

    tasks.forEach { task ->
      assertTrue("Task ID must not be blank", task.id.isNotBlank())
      assertTrue("Task title must not be blank", task.title.isNotBlank())
      assertTrue("Task difficulty must be between 1 and 5", task.difficulty in 1..5)
      assertTrue("Task description must have substance", task.description.length > 10)
      assertTrue("Evidence requirements must be explicit", task.evidenceRequirements.isNotBlank())
    }
  }

  @Test
  fun testMilestonesIntegrity() {
    val milestones = SeedMissionCatalog.realMilestones
    assertTrue("Milestones catalog should have items", milestones.isNotEmpty())

    milestones.forEach { ms ->
      assertTrue("Target value must be positive", ms.targetValue > 0)
      assertTrue("Progress percent must be bounded", ms.percent in 0f..1f)
    }
  }

  @Test
  fun testProfileProgressionModel() {
    val profile = SeedMissionCatalog.defaultProfile
    assertTrue("Level must be >= 1", profile.level >= 1)
    assertTrue("XP must be non-negative", profile.xp >= 0)
    assertTrue("Next level XP must exceed current XP", profile.nextLevelXp > profile.xp)
    assertTrue("Verified contributions must be non-negative", profile.verifiedContributionsCount >= 0)
    assertTrue("Badges should include unlocked and locked", profile.badges.any { it.isUnlocked } && profile.badges.any { !it.isUnlocked })
  }
}
