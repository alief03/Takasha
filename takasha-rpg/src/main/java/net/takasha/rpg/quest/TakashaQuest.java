package net.takasha.rpg.quest;

public class TakashaQuest {

    public enum QuestType {
        KEMAJUAN,
        DAILY,
        WEEKLY,
        LEGENDARY
    }

    private final String id;
    private final String code;
    private final String title;
    private final String description;
    private final QuestType type;
    private final int chapter; // 1-4 for KEMAJUAN, 0 for others
    private final int targetGoal;
    private final int rewardCoins;
    private final int rewardGems;
    private final String extraReward;

    public TakashaQuest(String id, String code, String title, String description, QuestType type, int chapter, int targetGoal, int rewardCoins, int rewardGems, String extraReward) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.description = description;
        this.type = type;
        this.chapter = chapter;
        this.targetGoal = targetGoal;
        this.rewardCoins = rewardCoins;
        this.rewardGems = rewardGems;
        this.extraReward = extraReward;
    }

    public TakashaQuest(String id, String code, String title, String description, QuestType type, int targetGoal, int rewardCoins, int rewardGems, String extraReward) {
        this(id, code, title, description, type, 0, targetGoal, rewardCoins, rewardGems, extraReward);
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public QuestType getType() {
        return type;
    }

    public int getChapter() {
        return chapter;
    }

    public int getTargetGoal() {
        return targetGoal;
    }

    public int getTarget() {
        return targetGoal;
    }

    public int getTargetAmount() {
        return targetGoal;
    }

    public int getRewardCoins() {
        return rewardCoins;
    }

    public int getRewardGems() {
        return rewardGems;
    }

    public String getExtraReward() {
        return extraReward;
    }

    public String getRewardSummary() {
        return extraReward;
    }

    public String getRewardItemSummary() {
        return extraReward;
    }
}
