package engine;

import screen.GameScreen;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import engine.SoundManager;
import java.util.LinkedList;
import java.util.Queue;


/**
 * Manages the list of achievements for a player,
 * including loading from and saving to the FileManager.
 */
public class AchievementManager {
    private static final java.util.logging.Logger logger = Core.getLogger();

    private List<Achievement> achievements;

    private  static AchievementManager instance;

    public AchievementManager() {
        this.achievements = createDefaultAchievements();
    }

    /** Defines the default achievements available in the game. */
    private List<Achievement> createDefaultAchievements() {
        List<Achievement> list = new ArrayList<>();
        list.add(new Achievement("First Blood", "Defeat your first enemy."));
        list.add(new Achievement("Survivor", "Clear a round without losing a life."));
        list.add(new Achievement("Clear", "Clear 5 levels."));
        list.add(new Achievement("Sharpshooter", "Record an accuracy of more than 80 percent"));
        list.add(new Achievement("50 Bullets", "Fire 50 Bullets."));
        list.add(new Achievement("Get 3000 Score", "Get more than 3,000 points"));
        list.add(new Achievement("Perfect Shooter", "Destroy all enemies with perfect accuracy."));
        return list;
    }

    /**
     * Loads the achievements from FileManager using a boolean list
     * and converts them into Achievement objects.
     */
    public void loadFromBooleans(String userName) throws IOException {
        List<Boolean> flags = FileManager.getInstance().searchAchievementsByName(userName);
        this.achievements = createDefaultAchievements();
        for (int i = 0; i < flags.size() && i < achievements.size(); i++) {
            if (flags.get(i)) {
                achievements.get(i).unlock();
            }
        }
    }

    /**
     * Converts the achievements into a boolean list and
     * saves them using FileManager.
     */
    public void saveToFile(String userName, String mode) throws IOException {
        List<Boolean> flags = new ArrayList<>();
        for (Achievement a : achievements) {
            flags.add(a.isUnlocked());
        }
        FileManager.getInstance().unlockAchievement(userName, flags, mode); // mode 추가
    }

    /** Returns the current achievement list. */
    public List<Achievement> getAchievements() {
        return achievements;
    }

    /** Unlocks the achievement by name. */
    public void unlock(String name) {
        for (Achievement a : achievements) {
            if (a.getName().equals(name) && !a.isUnlocked()) {
                a.unlock();
                SoundManager.playOnce("sound/achievement.wav");
                logger.info("Achievement unlocked: " + a);
                toastQueue.offer(new Toast(a, TOAST_DURATION_MS));
            }
        }
    }
    private final Queue<Toast> toastQueue = new LinkedList<>();
    private Toast activeToast = null;
    private static final int TOAST_DURATION_MS = 3000;

    public void update() {
        if (activeToast == null || !activeToast.alive()) {
            activeToast = toastQueue.poll();
            if (activeToast != null) {
                activeToast.ttl.reset();
            }
        }
    }


    public List<Achievement> getActiveToasts() {
        List<Achievement> activeList = new ArrayList<>();
        if (activeToast != null && activeToast.alive()) {
            activeList.add(activeToast.achievement);
        }
        return activeList;
    }

    /**
     * Make sure you have a pop-up on the screen, or you have a pop-up left in the queue.
     * @return If there is at least one pop-up left, true, or false
     */
    public boolean hasPendingToasts() {
        return (activeToast != null && activeToast.alive()) || !toastQueue.isEmpty();
    }

    private static final class Toast {
        final Achievement achievement;
        final Cooldown ttl;

        Toast(Achievement achievement, int ms) {
            this.achievement = achievement;
            this.ttl = Core.getCooldown(ms);
        }

        boolean alive() {
            return !ttl.checkFinished();
        }
    }

    /**
     * Returns the shared instance of AchievementManager.
     * [2025-10-17] Added in commit feat: complete drawAchievementMenu method in DrawManager.
     */
    protected static AchievementManager getInstance() {
        if (instance == null)
            instance = new AchievementManager();
        return instance;
    }
}
