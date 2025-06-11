package com.westerhoud.osrs.taskman.api;

import com.westerhoud.osrs.taskman.RequestCallback;
import com.westerhoud.osrs.taskman.domain.AccountCredentials;
import com.westerhoud.osrs.taskman.domain.AccountProgress;
import com.westerhoud.osrs.taskman.domain.Task;
import com.westerhoud.osrs.taskman.domain.TaskmanCommandData;
import java.io.IOException;

public interface TaskService {

  void getCurrentTask(final AccountCredentials credentials, final String name, RequestCallback<Task> rc)
      throws IllegalArgumentException;

  void generateTask(final AccountCredentials credentials, final String name, RequestCallback<Task> rc)
      throws IllegalArgumentException;

  void completeTask(final AccountCredentials credentials, final String name, RequestCallback<Task> rc)
      throws IllegalArgumentException;

  void getAccountProgress(AccountCredentials credentials, String rsn, RequestCallback<AccountProgress> rc)
      throws IllegalArgumentException;

  TaskmanCommandData getChatCommandData(final String rsn) throws IOException;
}
