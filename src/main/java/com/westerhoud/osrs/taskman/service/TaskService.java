package com.westerhoud.osrs.taskman.service;

import com.google.gson.Gson;
import com.westerhoud.osrs.taskman.RequestCallback;
import com.westerhoud.osrs.taskman.domain.AccountCredentials;
import com.westerhoud.osrs.taskman.domain.AccountProgress;
import com.westerhoud.osrs.taskman.domain.ErrorResponse;
import com.westerhoud.osrs.taskman.domain.Task;
import com.westerhoud.osrs.taskman.domain.TaskmanCommandData;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
public class TaskService implements com.westerhoud.osrs.taskman.api.TaskService {

  public static final String TASKMAN_IDENTIFIER_HEADER = "x-taskman-identifier";
  public static final String TASKMAN_PASSWORD_HEADER = "x-taskman-password";
  public static final String TASKMAN_SOURCE_HEADER = "x-taskman-source";
  public static final String TASKMAN_RSN_HEADER = "x-taskman-rsn";
  private static final String BASE_URL = "https://taskman.up.railway.app/task";
  private final OkHttpClient client;
  private final Gson gson;
  private final String currentUrl;
  private final String generateUrl;
  private final String completeUrl;
  private final String progressUrl;
  private final String commandUrl;

  public TaskService(final OkHttpClient okHttpClient, final Gson gson) {
    client = okHttpClient;
    this.gson = gson;
    currentUrl = BASE_URL + "/current";
    generateUrl = BASE_URL + "/generate";
    completeUrl = BASE_URL + "/complete";
    progressUrl = BASE_URL + "/progress";
    commandUrl = BASE_URL + "/command/%s";
  }

  @Override
  public void getCurrentTask(final AccountCredentials credentials, final String rsn, RequestCallback<Task> rc)
      throws IllegalArgumentException {
    checkCredentials(credentials);

    final Request request =
        new Request.Builder()
            .url(currentUrl)
            .addHeader(TASKMAN_IDENTIFIER_HEADER, credentials.getIdentifier())
            .addHeader(TASKMAN_PASSWORD_HEADER, credentials.getPassword())
            .addHeader(TASKMAN_SOURCE_HEADER, credentials.getSource().name())
            .addHeader(TASKMAN_RSN_HEADER, rsn)
            .get()
            .build();

    executeRequestAsync(request, new RequestCallback<Task>() {
      @Override
      public void onSuccess(@NonNull final Task res) {
        setImage(res, rc);
      }

      @Override
      public void onFailure(@NonNull final Exception e) {
        rc.onFailure(e);
      }
    }, Task.class);
  }

  @Override
  public void generateTask(final AccountCredentials credentials, final String rsn, RequestCallback<Task> rc) {
    final Request request =
        new Request.Builder()
            .url(generateUrl)
            .header("Content-Type", "application/json")
            .addHeader(TASKMAN_SOURCE_HEADER, credentials.getSource().name())
            .addHeader(TASKMAN_RSN_HEADER, rsn)
            .post(getRequestBody(credentials))
            .build();

    executeRequestAsync(request, new RequestCallback<Task>() {
      @Override
      public void onSuccess(@NonNull final Task res) {
        setImage(res, rc);
        rc.onSuccess(res);
      }

      @Override
      public void onFailure(@NonNull final Exception e) {
        rc.onFailure(e);
      }
    }, Task.class);
  }

  @Override
  public void completeTask(final AccountCredentials credentials, final String rsn, RequestCallback<Task> rc) {
    final Request request =
        new Request.Builder()
            .url(completeUrl)
            .header("Content-Type", "application/json")
            .addHeader(TASKMAN_SOURCE_HEADER, credentials.getSource().name())
            .addHeader(TASKMAN_RSN_HEADER, rsn)
            .post(getRequestBody(credentials))
            .build();

    executeRequestAsync(request, new RequestCallback<Task>() {
      @Override
      public void onSuccess(@NonNull final Task res) {
        setImage(res, rc);
        rc.onSuccess(res);
      }

      @Override
      public void onFailure(@NonNull final Exception e) {
        rc.onFailure(e);
      }
    }, Task.class);
  }

  @Override
  public void getAccountProgress(final AccountCredentials credentials, final String rsn,
      final RequestCallback<AccountProgress> rc) throws IllegalArgumentException {
    checkCredentials(credentials);

    final Request request =
        new Request.Builder()
            .url(progressUrl)
            .addHeader(TASKMAN_IDENTIFIER_HEADER, credentials.getIdentifier())
            .addHeader(TASKMAN_PASSWORD_HEADER, credentials.getPassword())
            .addHeader(TASKMAN_SOURCE_HEADER, credentials.getSource().name())
            .addHeader(TASKMAN_RSN_HEADER, rsn)
            .get()
            .build();

    executeRequestAsync(request, rc, AccountProgress.class);
  }

@Override
  public TaskmanCommandData getChatCommandData(final String rsn) throws IOException {
    final Request request = new Builder().url(String.format(commandUrl, rsn)).get().build();

    final Response response = client.newCall(request).execute();

    if (response.code() == 200) {
      return gson.fromJson(response.body().string(), TaskmanCommandData.class);
    } else {
      throw new IllegalArgumentException("Could not get task command data for rsn: " + rsn);
    }
  }

  private <T> void executeRequestAsync(final Request request, final RequestCallback<T> rc, Class<T> clazz) {
    client.newCall(request).enqueue(new Callback() {
      @Override
      public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
        if (response.code() == 200 && response.body() != null) {
          rc.onSuccess(gson.fromJson(response.body().string(), clazz));
          return;
        }

        final ErrorResponse error = mapResponseToErrorResponse(response);
        rc.onFailure(new IllegalArgumentException(error.getMessage()));
      }

      @Override
      public void onFailure(@NonNull Call call, @NonNull IOException e) {
        rc.onFailure(e);
      }
    });
  }

  private RequestBody getRequestBody(final AccountCredentials credentials) {
    return RequestBody.create(MediaType.parse("application/json"), gson.toJson(credentials));
  }

  private ErrorResponse mapResponseToErrorResponse(final Response response) throws IOException {
    final String responseString = response.body().string();
    log.error(responseString);
    return gson.fromJson(responseString, ErrorResponse.class);
  }

  private static void checkCredentials(final AccountCredentials credentials) {
    if (!credentials.isValid()) {
      throw new IllegalArgumentException("Please configure your credentials in the plugin configurations");
    }
  }

  private void setImage(final Task task, final RequestCallback<Task> rc) {
    final Request request = new Builder().url(task.getImageUrl()).get().build();

    client.newCall(request).enqueue(new Callback() {
      @Override
      public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
        final ResponseBody responseBody = response.body();

        if (responseBody == null) {
          log.info(task.getImageUrl());
          return;
        }

        task.setImage(ImageIO.read(responseBody.byteStream()));
        rc.onSuccess(task);
      }

      @Override
      public void onFailure(@NonNull Call call, @NonNull IOException e) {
        log.error(e.getMessage(), e);
        // we kinda don't care if the image doesn't load
        rc.onSuccess(task);
      }
    });
  }
}
