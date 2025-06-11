package com.westerhoud.osrs.taskman;

import lombok.NonNull;

public interface RequestCallback<T>
{
	void onSuccess(@NonNull T res);
	default void onFailure(@NonNull Exception e) {}
}

