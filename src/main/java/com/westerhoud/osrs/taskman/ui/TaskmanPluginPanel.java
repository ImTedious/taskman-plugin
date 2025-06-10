package com.westerhoud.osrs.taskman.ui;

import com.westerhoud.osrs.taskman.RequestCallback;
import com.westerhoud.osrs.taskman.TaskmanPlugin;
import com.westerhoud.osrs.taskman.domain.AccountProgress;
import com.westerhoud.osrs.taskman.domain.Task;
import com.westerhoud.osrs.taskman.domain.TierProgress;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ColorJButton;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.ui.components.ProgressBar;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

@Slf4j
public class TaskmanPluginPanel extends PluginPanel {

  private final TaskmanPlugin taskmanPlugin;
  private final ClientThread clientThread;
  private final JPanel taskPanel;
  private final JPanel taskDataPanel;
  private final JPanel progressPanel;
  private final PluginErrorPanel errorPanel;
  private final JShadowedLabel currentTaskLabel = new JShadowedLabel("Current task:");
  private final JShadowedLabel progressLabel = new JShadowedLabel("Progress:");
  private final JShadowedLabel imageLabel = new JShadowedLabel();
  private final JShadowedLabel nameLabel = new JShadowedLabel();
  private final JButton refreshButton = new JButton();
  private final ColorJButton generateButton =
      new ColorJButton("Generate task", ColorScheme.DARK_GRAY_COLOR);
  private final ColorJButton completeButton =
      new ColorJButton("Complete task", ColorScheme.DARK_GRAY_COLOR);
  private final ColorJButton faqButton =
      new ColorJButton("FAQ", ColorScheme.DARKER_GRAY_COLOR);
  private final JPanel tryAgainPanel;

  public TaskmanPluginPanel(final TaskmanPlugin taskmanPlugin, final ClientThread clientThread) {
    super();
    this.clientThread = clientThread;
    this.taskmanPlugin = taskmanPlugin;

    setOpaque(false);
    setBorder(new EmptyBorder(50, 0, 0, 0));
    setLayout(new BorderLayout());

    taskPanel = new JPanel(new BorderLayout(10, 10));
    taskPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
    taskPanel.setVisible(false);

    taskDataPanel = new JPanel(new BorderLayout(10, 5));
    final JPanel taskDataTitlePanel = new JPanel(new BorderLayout());
    currentTaskLabel.setFont(FontManager.getRunescapeFont());
    currentTaskLabel.setForeground(Color.WHITE);
    refreshButton.setIcon(getRefreshButton());
    refreshButton.setPreferredSize(new Dimension(25, 25));
    refreshButton.setMaximumSize(new Dimension(25, 25));
    refreshButton.setFocusPainted(false);
    refreshButton.addActionListener(e -> reset());
    taskDataTitlePanel.add(currentTaskLabel, BorderLayout.WEST);
    taskDataTitlePanel.add(refreshButton, BorderLayout.EAST);
    nameLabel.setFont(FontManager.getRunescapeSmallFont());
    nameLabel.setHorizontalAlignment(SwingConstants.LEFT);
    taskDataPanel.add(taskDataTitlePanel, BorderLayout.NORTH);
    taskDataPanel.add(imageLabel, BorderLayout.WEST);
    taskDataPanel.add(nameLabel, BorderLayout.CENTER);

    final JPanel buttonPanel = new JPanel(new BorderLayout(10, 10));
    generateButton.setFont(FontManager.getRunescapeSmallFont());
    generateButton.setFocusPainted(false);
    generateButton.addActionListener(e -> generateTaskAndUpdateContent());
    completeButton.setFont(FontManager.getRunescapeSmallFont());
    completeButton.setFocusPainted(false);
    completeButton.addActionListener(e -> completeTaskAndUpdateContent());
    faqButton.setFont(FontManager.getRunescapeSmallFont());
    faqButton.setFocusPainted(false);
    faqButton.addActionListener(e -> LinkBrowser.browse("https://docs.google.com/document/d/e/2PACX-1vTHfXHzMQFbt_iYAP-O88uRhhz3wigh1KMiiuomU7ftli-rL_c3bRqfGYmUliE1EHcIr3LfMx2UTf2U/pub"));
    buttonPanel.add(generateButton, BorderLayout.WEST);
    buttonPanel.add(completeButton, BorderLayout.CENTER);
    buttonPanel.add(faqButton, BorderLayout.SOUTH);

    progressPanel = new JPanel(new GridLayout(5, 1, 10, 10));
    progressPanel.setBorder(new EmptyBorder(30, 10, 0, 10));
    progressPanel.setVisible(false);

    progressLabel.setFont(FontManager.getRunescapeFont());
    progressLabel.setForeground(Color.WHITE);

    taskPanel.add(taskDataPanel, BorderLayout.NORTH);
    taskPanel.add(buttonPanel, BorderLayout.CENTER);
    taskPanel.add(progressPanel, BorderLayout.SOUTH);

    errorPanel = new PluginErrorPanel();
    errorPanel.setBorder(new EmptyBorder(50, 0, 0, 0));
    tryAgainPanel = new JPanel();
    final JButton tryAgainButton = new JButton("Try again");
    tryAgainButton.setFocusPainted(false);
    tryAgainButton.addActionListener(e -> reset());
    tryAgainButton.setPreferredSize(new Dimension(100, 25));
    tryAgainButton.setMaximumSize(new Dimension(150, 25));
    tryAgainPanel.add(tryAgainButton);
    tryAgainPanel.setVisible(false);
    errorPanel.add(tryAgainPanel, BorderLayout.SOUTH);
    errorPanel.setContent("Please login first!", "");
    add(taskPanel, BorderLayout.NORTH);
    add(progressPanel, BorderLayout.CENTER);
    add(errorPanel, BorderLayout.SOUTH);
  }

  public void init() {
    reset();
  }

  private void updateTaskPanelContent(final Task task) {
    imageLabel.setIcon(new ImageIcon(task.getResizedImage(25, 25)));
    nameLabel.setText(task.getName());
    taskPanel.setVisible(true);
  }

  private void showErrorMessage(final Exception e) {
    log.error(e.getMessage(), e);

    tryAgainPanel.setVisible(true);
    errorPanel.setContent("Oops... Something went wrong", e.getMessage());
    errorPanel.setVisible(true);
    errorPanel.revalidate();
    errorPanel.repaint();
  }

  private void getCurrentTaskAndUpdateContent() {
    try {
      taskmanPlugin.getCurrentTask(new RequestCallback<Task>() {
        @Override
        public void onSuccess(@NonNull final Task res) {
          clientThread.invoke(() -> {
            updateTaskPanelContent(res);
            errorPanel.setVisible(false);
          });
        }

        @Override
        public void onFailure(@NonNull final Exception e) {
          clientThread.invoke(() -> showErrorMessage(e));
        }
      });
    } catch (final Exception e) {
      showErrorMessage(e);
    }
  }

  private void generateTaskAndUpdateContent() {
    try {
      taskmanPlugin.generateTask(new RequestCallback<Task>() {
        @Override
        public void onSuccess(@NonNull final Task res) {
          clientThread.invoke(() -> {
            updateTaskPanelContent(res);
            errorPanel.setVisible(false);
          });
        }

        @Override
        public void onFailure(@NonNull final Exception e) {
          clientThread.invoke(() -> showErrorMessage(e));
        }
      });
    } catch (final Exception e) {
      showErrorMessage(e);
    }
  }

  private void completeTaskAndUpdateContent() {
    try {
      taskmanPlugin.completeTask(new RequestCallback<Task>() {
        @Override
        public void onSuccess(@NonNull final Task res) {
          clientThread.invoke(() -> updateTaskPanelContent(res));
          getProgressAndUpdateContent(new RequestCallback<AccountProgress>() {
            @Override
            public void onSuccess(@NonNull final AccountProgress _res) {
              clientThread.invoke(() -> errorPanel.setVisible(false));
            }

            @Override
            public void onFailure(@NonNull final Exception e) {
              clientThread.invoke(() -> showErrorMessage(e));
            }
          });
        }

        @Override
        public void onFailure(@NonNull final Exception e) {
          clientThread.invoke(() -> showErrorMessage(e));
        }
      });
    } catch (final Exception e) {
      showErrorMessage(e);
    }
  }

  private void getProgressAndUpdateContent(RequestCallback<AccountProgress> rc) {
    try {
      taskmanPlugin.progress(
        new RequestCallback<AccountProgress>() {
          @Override
          public void onSuccess(final @NonNull AccountProgress res) {
            clientThread.invoke(() -> {
              updateProgressContent(res);
              rc.onSuccess(res);
            });
          }

          @Override
          public void onFailure(final @NonNull Exception e) {
            clientThread.invoke(() -> {
              showErrorMessage(e);
              rc.onFailure(e);
            });
          }
        }
      );
    } catch (Exception e) {
      showErrorMessage(e);
    }
  }

  private void updateProgressContent(final AccountProgress accountProgress) {
    progressPanel.removeAll();
    progressPanel.add(progressLabel);
    for (final Map.Entry<String, TierProgress> entry :
        accountProgress.getProgressByTier().entrySet()) {
      final String key = entry.getKey();
      final TierProgress value = entry.getValue();
      final ProgressBar progressBar = new ProgressBar();
      progressBar.setMaximumValue(value.getMaxValue());
      progressBar.setValue(value.getValue());
      progressBar.setRightLabel(String.valueOf(value.getMaxValue()));
      progressBar.setLeftLabel(String.valueOf(value.getValue()));
      final int percentage = progressBar.getPercentage();
      progressBar.setCenterLabel(String.format("%s %d%%", key, percentage));
      progressBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
      if (percentage == 0) {
        progressBar.setForeground(Color.RED);
      } else if (percentage < 25) {
        progressBar.setForeground(Color.decode("#ea6600"));
      } else if (percentage < 50) {
        progressBar.setForeground(Color.decode("#ffb600"));
      } else if (percentage < 75) {
        progressBar.setForeground(Color.decode("#ffe500"));
      } else if (percentage < 100) {
        progressBar.setForeground(Color.decode("#aeff00"));
      } else {
        progressBar.setForeground(Color.GREEN);
      }
      progressPanel.add(progressBar);
    }
    if (!accountProgress.getProgressByTier().isEmpty()) {
      progressPanel.setVisible(true);
    }
  }

  private Icon getRefreshButton() {
    final BufferedImage image = ImageUtil.loadImageResource(getClass(), "refresh.png");
    final Image resizedImage = image.getScaledInstance(25, 25, Image.SCALE_FAST);
    return new ImageIcon(resizedImage);
  }

  public void reset() {
    taskPanel.setVisible(false);
    progressPanel.setVisible(false);
    getCurrentTaskAndUpdateContent();
    getProgressAndUpdateContent(res -> {});
  }

  public void onLogout() {
    taskPanel.setVisible(false);
    progressPanel.setVisible(false);
    tryAgainPanel.setVisible(false);
    errorPanel.setContent("Please login first!", "");
    errorPanel.setVisible(true);
    errorPanel.revalidate();
    errorPanel.repaint();
  }
}
