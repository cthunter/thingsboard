/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.js.NashornJsEngine;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import javax.script.Bindings;
import java.util.Set;

import static org.thingsboard.rule.engine.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "switch", customRelations = true,
        configClazz = TbJsSwitchNodeConfiguration.class,
        nodeDescription = "Route incoming Message to one or multiple output chains",
        nodeDetails = "Node executes configured JS script. Script should return array of next Chain names where Message should be routed. " +
                "If Array is empty - message not routed to next Node. " +
                "Message payload can be accessed via <code>msg</code> property. For example <code>msg.temperature < 10;</code> " +
                "Message metadata can be accessed via <code>metadata</code> property. For example <code>metadata.customerName === 'John';</code>",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeSwitchConfig")
public class TbJsSwitchNode implements TbNode {

    private TbJsSwitchNodeConfiguration config;
    private NashornJsEngine jsEngine;

    @Override
    public void init(TbNodeConfiguration configuration, TbNodeState state) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbJsSwitchNodeConfiguration.class);
        this.jsEngine = new NashornJsEngine(config.getJsScript(), "Switch");
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ListeningExecutor jsExecutor = ctx.getJsExecutor();
        withCallback(jsExecutor.executeAsync(() -> jsEngine.executeSwitch(toBindings(msg))),
                result -> processSwitch(ctx, msg, result),
                t -> ctx.tellError(msg, t));
    }

    private void processSwitch(TbContext ctx, TbMsg msg, Set<String> nextRelations) {
        ctx.tellNext(msg, nextRelations);
    }

    private Bindings toBindings(TbMsg msg) {
        return NashornJsEngine.bindMsg(msg);
    }

    @Override
    public void destroy() {
        if (jsEngine != null) {
            jsEngine.destroy();
        }
    }
}
