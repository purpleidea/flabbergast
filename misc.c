#include<stdlib.h>
#include<stdio.h>
#include<glib.h>

static gchar **paths = NULL;
static gint paths_length = 0;

static void init_paths(
	) {
	static gsize have_searched = 0;

	if (g_once_init_enter(&have_searched)) {
		const char *path = getenv("FLABBERGAST_PATH");
		if (path != NULL) {
			paths = g_strsplit(path, ":", 0);
			for (; paths[paths_length] != NULL; paths_length++) ;
		}
		paths_length += 3;
		paths = g_renew(gchar *, paths, paths_length);
		paths[paths_length - 3] = DATA_DIR "/flabbergast/lib";
		paths[paths_length - 2] = "/usr/flabbergast/lib";
		paths[paths_length - 1] = "/usr/local/flabbergast/lib";
		g_once_init_leave(&have_searched, 1);
	}
}

void flabbergast_add_library_search_path(
	const char *path) {
	gint it;
	init_paths();
	paths_length++;
	paths = g_renew(gchar *, paths, paths_length);
	for (it = paths_length - 1; it > 1; it--) {
		paths[it] = paths[it - 1];
	}
	paths[0] = g_strdup(path);
}

char *flabbergast_find_library(
	const char *lib_name) {
	gint it;
	init_paths();
	for (it = 0; it < paths_length; it++) {
		char file_name[1024];
		if (snprintf(file_name, sizeof(file_name), "%s/%s.flbgst", paths[it], lib_name) < sizeof(file_name)) {
			if (g_file_test(file_name, (G_FILE_TEST_EXISTS | G_FILE_TEST_IS_REGULAR))) {
				return g_strdup(file_name);
			}
		}
	}
	return NULL;
}
