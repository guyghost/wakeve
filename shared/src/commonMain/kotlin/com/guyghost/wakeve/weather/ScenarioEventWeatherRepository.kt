package com.guyghost.wakeve.weather

import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.repository.ScenarioRepository

class ScenarioEventWeatherRepository(
    private val scenarioRepository: ScenarioRepository
) : EventWeatherScenarioRepository {
    override fun getSelectedScenario(eventId: String): Scenario? {
        return scenarioRepository.getSelectedScenario(eventId)
    }

    override fun getScenarios(eventId: String): List<Scenario> {
        return scenarioRepository.getScenariosByEventId(eventId)
    }
}
