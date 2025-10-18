package screen;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.sql.Array;
import java.util.*;

import engine.*;

/**
 * Implements the score screen.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class ScoreScreen extends Screen {

    /** Milliseconds between changes in user selection. */
    private static final int SELECTION_TIME = 200;
    /** Code of first mayus character. */
    private static final int FIRST_CHAR = 65;
    /** Code of last mayus character. */
    private static final int LAST_CHAR = 90;
    /** Code of max high score. */
    private static final int MAX_HIGH_SCORE_NUM = 7;

    // Added for persist per-player breakdown
    private final GameState gameState;

    /** Current score. */
    private int score;
    /** Player lives left. */
    private int livesRemaining;
    /** Total bullets shot by the player. */
    private int bulletsShot;
    /** Total ships destroyed by the player. */
    private int shipsDestroyed;
    /** List of past high scores. */
    private List<Score> highScores;
    /** Checks if current score is a new high score. */
    private boolean isNewRecord;
    /** Player name for record input. */
    private char[] name;
    /** Character of players name selected for change. */
    private int nameCharSelected;
    /** Time between changes in user selection. */
    private Cooldown selectionCooldown;
    /** manages achievements.*/
    private AchievementManager achievementManager;
    /** Total coins earned in the game. */
    private int[] totalCoins = new int[2];
    /** check 1P/2P mode; */
    private String mode;

    /**
     * Constructor, establishes the properties of the screen.
     *
     * @param width
     *                  Screen width.
     * @param height
     *                  Screen height.
     * @param fps
     *                  Frames per second, frame rate at which the game is run.
     * @param gameState
     *                  Current game state.
     * @param achievementManager
     * 			            Achievement manager instance used to track and save player achievements.
     * 			  2025-10-03  add generator parameter and comment
     */
    public ScoreScreen(final int width, final int height, final int fps,
                       final GameState gameState, final AchievementManager achievementManager) throws IOException {
        super(width, height, fps);
        this.gameState = gameState; // Added

        this.score = gameState.getScore();
        this.livesRemaining = gameState.getLivesRemaining();
        this.bulletsShot = gameState.getBulletsShot();
        this.shipsDestroyed = gameState.getShipsDestroyed();
        this.totalCoins[0] = gameState.getCoins(); // ADD THIS LINE
        this.isNewRecord = true;
        this.name = "AAA".toCharArray();
        this.nameCharSelected = 0;
        this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
        this.selectionCooldown.reset();
        this.achievementManager = achievementManager;
        this.mode = gameState.getCoop() ? "2P" : "1P";

        try {
            this.highScores = Core.getFileManager().loadHighScores(this.mode);
            if (highScores.size() < MAX_HIGH_SCORE_NUM
                    || highScores.get(highScores.size() - 1).getScore() < this.score)
                this.isNewRecord = true;

        } catch (IOException e) {
            logger.warning("Couldn't load high scores!");
        }
    }

    /**
     * Starts the action.
     *
     * @return Next screen code.
     */
    public final int run() {
        super.run();

        return this.returnCode;
    }

    /**
     * Updates the elements on screen and checks for events.
     */
    protected final void update() {
        super.update();

		draw();
		if (this.inputDelay.checkFinished()) {
			if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
                // Return to main menu.
                SoundManager.playOnce("sound/select.wav");
				this.returnCode = 1;
				this.isRunning = false;
				if (this.isNewRecord) {
					saveScore();
					saveAchievement(); //2025-10-03 call method for save achievement released
				}
			} else if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
				// Play again.
                SoundManager.playOnce("sound/select.wav");
				this.returnCode = 2;
				this.isRunning = false;
				if (this.isNewRecord) {
					saveScore();
					saveAchievement(); // 2025-10-03 call method for save achievement released
				}
			}

			if (this.selectionCooldown.checkFinished()) {
				if (inputManager.isKeyDown(KeyEvent.VK_RIGHT)) {
                    SoundManager.playOnce("sound/hover.wav");
					this.nameCharSelected = this.nameCharSelected == 2 ? 0
							: this.nameCharSelected + 1;
					this.selectionCooldown.reset();
				}
				if (inputManager.isKeyDown(KeyEvent.VK_LEFT)) {
                    SoundManager.playOnce("sound/hover.wav");
					this.nameCharSelected = this.nameCharSelected == 0 ? 2
							: this.nameCharSelected - 1;
					this.selectionCooldown.reset();
				}
				if (inputManager.isKeyDown(KeyEvent.VK_UP)) {
                    SoundManager.playOnce("sound/hover.wav");
					this.name[this.nameCharSelected] = (char) (this.name[this.nameCharSelected] == LAST_CHAR
							? FIRST_CHAR
							: this.name[this.nameCharSelected] + 1);
					this.selectionCooldown.reset();
				}
				if (inputManager.isKeyDown(KeyEvent.VK_DOWN)) {
                    SoundManager.playOnce("sound/hover.wav");
					this.name[this.nameCharSelected] = (char) (this.name[this.nameCharSelected] == FIRST_CHAR
							? LAST_CHAR
							: this.name[this.nameCharSelected] - 1);
					this.selectionCooldown.reset();
				}
			}
		}

    }

    /**
     * Saves the score as a high score.
     * 2025-10-18
     * Add ability that distinguish duplicate names and save higher scores
     */
    private void saveScore() {
        String mode = (gameState != null && gameState.isCoop()) ? "2P" : "1P";
        String newName = new String(this.name);
        Score newScore = new Score(newName, this.gameState, mode);
        boolean foundAndReplaced = false;
        for (int i = 0; i < highScores.size(); i++) {
            Score existingScore = highScores.get(i);
            if (existingScore.getName().equals(newName)) {
                if (newScore.getScore() > existingScore.getScore()) {
                    highScores.set(i, newScore);
                    foundAndReplaced = true;
                } else {
                    foundAndReplaced = true;
                }
                break;
            }
        }
        if (!foundAndReplaced) {
            highScores.add(newScore);
        }
        Collections.sort(highScores);
        if (highScores.size() > MAX_HIGH_SCORE_NUM)
            highScores.remove(highScores.size() - 1);
        try {
            Core.getFileManager().saveHighScores(highScores, mode);
        } catch (IOException e) {
            logger.warning("Couldn't load high scores!");
        }
    }

    /**
     * Save the achievement released.
     * 2025-10-03
     * add new method
     */
    private void saveAchievement() {
        try {
            this.achievementManager.saveToFile(new String(this.name), this.mode);
        } catch (IOException e) {
            logger.warning("Couldn't save achievements!");
        }
    }

    /**
     * Draws the elements associated with the screen.
     */
    private void draw() {
        drawManager.initDrawing(this);

		drawManager.drawGameOver(this, this.inputDelay.checkFinished());

        float accuracy = (this.bulletsShot > 0)
                ? (float) this.shipsDestroyed / this.bulletsShot
                : 0f;

		// 2P mode: edit to include co-op + individual score/coins
		if (this.gameState != null && this.gameState.isCoop()) {
			// team summary
			drawManager.drawResults(this,
					this.gameState.getScore(), // team score
					this.gameState.getLivesRemaining(),
					this.gameState.getShipsDestroyed(),
					0f, // leaving out team accuracy
                    false // Draw accuracy for 2P mode
			);

            // show per-player lines when in 2P mode

            float p1Acc = this.gameState.getBulletsShot(0) > 0 ? (float) this.gameState.getShipsDestroyed(0) / this.gameState.getBulletsShot(0) : 0f;
            float p2Acc = this.gameState.getBulletsShot(1) > 0 ? (float) this.gameState.getShipsDestroyed(1) / this.gameState.getBulletsShot(1) : 0f;

            String p1 = String.format("P1  %04d  |  acc %.2f%%", this.gameState.getScore(0), p1Acc * 100f);
            String p2 = String.format("P2  %04d  |  acc %.2f%%", this.gameState.getScore(1), p2Acc * 100f);

            int y;  // tweak these if you want
            if (this.isNewRecord) {
                y = this.getHeight() / 2 - 5; // Position if new record is True
            } else {
                y = this.getHeight() / 2 + 60; // Position if new record is False
            }
            drawManager.drawCenteredRegularString(this, p1, y);
            drawManager.drawCenteredRegularString(this, p2, y + 40); // Increase spacing

		} else {
			// 1P legacy summary with accuracy
			float acc = (this.bulletsShot > 0) ? (float) this.shipsDestroyed / this.bulletsShot : 0f;
			drawManager.drawResults(this, this.score, this.livesRemaining, this.shipsDestroyed, acc, true); // Draw accuracy for 1P mode
		}

        drawManager.drawNameInput(this, this.name, this.nameCharSelected, isNewRecord);

        drawManager.completeDrawing(this);
    }

}