package net.takasha.rpg.mythic;

import java.util.HashMap;
import java.util.Map;

/**
 * Executes a nested or referenced MythicMobs subskill with parameter overrides.
 */
public class SubSkillMechanic implements MythicMechanic {

    private final String skillName;
    private final Map<String, String> parameterOverrides = new HashMap<>();

    public SubSkillMechanic(Map<String, String> params) {
        this.skillName = params.getOrDefault("s", params.getOrDefault("skill", ""));
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String k = entry.getKey();
            if (!k.equals("s") && !k.equals("skill")) {
                parameterOverrides.put(k, entry.getValue());
            }
        }
    }

    @Override
    public void execute(SkillExecutionContext context) {
        if (skillName == null || skillName.isBlank()) return;

        SkillExecutionContext childContext = context.copy();
        for (Map.Entry<String, String> entry : parameterOverrides.entrySet()) {
            String evaluated = childContext.parseString(entry.getValue());
            childContext.setVariable(entry.getKey(), evaluated);
        }

        // Evaluate origin parameter if specified (e.g. origin=@SelfEyeLocation{fo=-0.5;yo=-0.2})
        String originTargeterStr = parameterOverrides.get("origin");
        if (originTargeterStr != null && !originTargeterStr.isBlank()) {
            SkillTargeter originTargeter = SkillTargeter.parse(originTargeterStr);
            java.util.List<SkillTargeter.TargetResult> origins = originTargeter.resolve(childContext);
            if (!origins.isEmpty()) {
                SkillTargeter.TargetResult o = origins.get(0);
                if (o.position() != null) {
                    childContext.setOrigin(o.position());
                } else if (o.entity() != null) {
                    childContext.setOrigin(o.entity().position());
                }
            }
        }

        MythicSkillExecutor.getInstance().executeSkill(skillName, childContext);
    }
}
