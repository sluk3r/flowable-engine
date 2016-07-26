/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.dmn.engine.impl;

import java.util.Map;

import org.activiti.dmn.api.DecisionTable;
import org.activiti.dmn.api.DmnRuleService;
import org.activiti.dmn.api.RuleEngineExecutionResult;
import org.activiti.dmn.engine.ActivitiDmnObjectNotFoundException;
import org.activiti.dmn.engine.RuleEngineExecutor;
import org.activiti.dmn.engine.impl.persistence.deploy.DecisionTableCacheEntry;
import org.activiti.dmn.engine.impl.persistence.deploy.DeploymentManager;
import org.activiti.dmn.model.DmnDefinition;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Yvo Swillens
 */
public class DmnRuleServiceImpl extends ServiceImpl implements DmnRuleService {

    protected RuleEngineExecutor ruleEngineExecutor;

    @Override
    public RuleEngineExecutionResult executeDecisionByKey(String decisionKey, Map<String, Object> processVariables) {
        return executeDecisionByKeyAndTenantId(decisionKey, processVariables, null);
    }

    @Override
    public RuleEngineExecutionResult executeDecisionByKeyAndTenantId(String decisionKey, Map<String, Object> processVariables, String tenantId) {

        DeploymentManager deploymentManager = engineConfig.getDeploymentManager();
        DecisionTable decisionTable = null;

        if (StringUtils.isNotEmpty(decisionKey) && StringUtils.isNotEmpty(tenantId)) {
            decisionTable = deploymentManager.findDeployedLatestDecisionByKeyAndTenantId(decisionKey, tenantId);
            if (decisionTable == null) {
                throw new ActivitiDmnObjectNotFoundException("No decision found for key: " + decisionKey + " and tenant ID: " + tenantId);
            }
        } else if (StringUtils.isNotEmpty(decisionKey)) {
            decisionTable = deploymentManager.findDeployedLatestDecisionByKey(decisionKey);
            if (decisionTable == null) {
                throw new ActivitiDmnObjectNotFoundException("No decision found for key: " + decisionKey);
            }
        } else {
            throw new IllegalArgumentException("decisionKey is null");
        }

        DecisionTableCacheEntry decisionTableCacheEntry = deploymentManager.resolveDecisionTable(decisionTable);
        DmnDefinition dmnDefinition = decisionTableCacheEntry.getDmnDefinition();

        RuleEngineExecutionResult executionResult = getRuleEngineExecutor().execute(dmnDefinition, processVariables);
        if (executionResult != null && executionResult.getAuditTrail() != null) {
            executionResult.getAuditTrail().setDmnDeploymentId(decisionTable.getDeploymentId());
        }

        return executionResult;
    }

    protected RuleEngineExecutor getRuleEngineExecutor() {
        if (ruleEngineExecutor == null) {
            ruleEngineExecutor = new RuleEngineExecutorImpl(engineConfig);
        }
        return ruleEngineExecutor;
    }
}