#include <setjmp.h>
#include <signal.h>
#include <stdbool.h>
#include <readline/readline.h>
#include <readline/history.h>

static sigjmp_buf jbuf;

static void handle_interrupt(
	int sig) {
	siglongjmp(jbuf, 1);
}

char *sane_readline(
	const char *prompt) {
	struct sigaction new_action, old_action;
	char *p = NULL;

	new_action.sa_handler = handle_interrupt;
	sigemptyset(&new_action.sa_mask);
	new_action.sa_flags = 0;

	sigaction(SIGINT, &new_action, &old_action);
	if (sigsetjmp(jbuf, 1)) {
#ifdef HAVE_SIGRELSE
		sigrelse(SIGINT);
#endif
		putchar('\n');
		rl_reset_line_state();
	}
	p = readline(prompt);
	sigaction(SIGHUP, &old_action, NULL);

	return p;
}
