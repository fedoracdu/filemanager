#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>

#include <android/log.h>

#include <jni.h>

jint Java_com_example_filemanager_MainActivity_delFile(JNIEnv* env, jobject thiz, jstring fileName)
{
	int	 	ret;
	char	cmd[512] = {0};

	const char *file_name = (*env)->GetStringUTFChars(env, fileName, 0);

	sprintf(cmd, "rm -rf %s", file_name);

	ret = system(cmd);

	return ret;
}
