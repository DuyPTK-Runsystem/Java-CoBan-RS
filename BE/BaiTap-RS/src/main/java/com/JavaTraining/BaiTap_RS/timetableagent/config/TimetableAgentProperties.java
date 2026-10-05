package com.JavaTraining.BaiTap_RS.timetableagent.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.timetable")
public class TimetableAgentProperties {

    private boolean enabled;
    private Duration proposalTtl = Duration.ofMinutes(15);
    private Duration providerTimeout = Duration.ofSeconds(30);
    @Value("${app.ai.timetable.max-model-calls:3}")
    private int maxModelCalls;
    private int maxClassCount;
    private int maxContextCharacters;
    private int maxRequestCharacters;
    private int maxPreferencesCharacters;
    private int maxProposalEntries;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getProposalTtl() {
        return proposalTtl;
    }

    public void setProposalTtl(Duration proposalTtl) {
        this.proposalTtl = proposalTtl;
    }

    public Duration getProviderTimeout() {
        return providerTimeout;
    }

    public void setProviderTimeout(Duration providerTimeout) {
        this.providerTimeout = providerTimeout;
    }

    public int getMaxModelCalls() {
        return maxModelCalls;
    }

    public void setMaxModelCalls(int maxModelCalls) {
        this.maxModelCalls = maxModelCalls;
    }

    public int getMaxClassCount() {
        return maxClassCount;
    }

    public void setMaxClassCount(int maxClassCount) {
        this.maxClassCount = maxClassCount;
    }

    public int getMaxContextCharacters() {
        return maxContextCharacters;
    }

    public void setMaxContextCharacters(int maxContextCharacters) {
        this.maxContextCharacters = maxContextCharacters;
    }

    public int getMaxRequestCharacters() {
        return maxRequestCharacters;
    }

    public void setMaxRequestCharacters(int maxRequestCharacters) {
        this.maxRequestCharacters = maxRequestCharacters;
    }

    public int getMaxPreferencesCharacters() {
        return maxPreferencesCharacters;
    }

    public void setMaxPreferencesCharacters(int maxPreferencesCharacters) {
        this.maxPreferencesCharacters = maxPreferencesCharacters;
    }

    public int getMaxProposalEntries() {
        return maxProposalEntries;
    }

    public void setMaxProposalEntries(int maxProposalEntries) {
        this.maxProposalEntries = maxProposalEntries;
    }

    public boolean hasRequiredBounds() {
        return maxClassCount > 0 && maxContextCharacters > 0 && maxRequestCharacters > 0
                && maxPreferencesCharacters > 0 && maxProposalEntries > 0;
    }
}
