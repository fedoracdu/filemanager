/*
 * Copyright (c) Christos Zoulas 2003.
 * All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice immediately at the beginning of the file, without modification,
 *    this list of conditions, and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
#include <stdio.h>
#include <android/log.h>
#include <jni.h>

#include "magic.h"

#define MSG_TAG	"libmagic"

static int init_result = 1;
static struct magic_set *ms = NULL;
static const char *magicfile = NULL;

jstring Java_com_example_filemanager_MainActivity_getFileType(JNIEnv* env, jobject thiz, jstring fileName)
{
	const char *type;

	if (init_result)
		type = "init magic fail";
	else {
		type = magic_file(ms, (*env)->GetStringUTFChars(env, fileName, 0));
		if (!type)
			type = "unkown";
	}

	//magic_close(ms);

	return (*env)->NewStringUTF(env, type);

}
jint Java_com_example_filemanager_MainActivity_initMagic(JNIEnv* env, jobject thiz, jstring fileName)
{
	init_result = 1;

	magicfile = (*env)->GetStringUTFChars(env, fileName, 0);
	if (!magicfile) {
		__android_log_print(ANDROID_LOG_WARN, MSG_TAG, "magicfile is null");
		return -1;
	}

	__android_log_print(ANDROID_LOG_INFO, MSG_TAG, "%s", magicfile);

	ms = magic_open(MAGIC_NONE);
	if (!ms) {
		__android_log_print(ANDROID_LOG_WARN, MSG_TAG, "magic_open failed");
		return -1;
	}

	if (-1 == magic_load(ms, magicfile)) {
		__android_log_print(ANDROID_LOG_WARN, MSG_TAG, "magic_load failed");
		return -1;
	}

	init_result = 0;

	__android_log_print(ANDROID_LOG_INFO, MSG_TAG, "libmagic init ok");

	return 0;
}

jint Java_com_example_filemanager_MainActivity_uninitMagic(JNIEnv* env, jobject thiz)
{
	if (ms) {
		magic_close(ms);
		ms = NULL;
		__android_log_print(ANDROID_LOG_INFO, MSG_TAG, "magic_close");
	}

	return 0;
}
