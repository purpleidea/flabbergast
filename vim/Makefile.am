GROUPS = indent ftdetect syntax

registrydir = $(datadir)/vim/registry
registry_DATA = vim-syntax-flabbergast.yaml

vim-syntax-flabbergast.yaml: Makefile
	echo addon: flabbergast-syntax > vim-syntax-flabbergast.yaml
	echo description: "Addon to highlight Flabbergast" >> vim-syntax-flabbergast.yaml
	echo files: >> vim-syntax-flabbergast.yaml
	for each in $(GROUPS); do echo " - $${each}/flabbergast.vim" >> vim-syntax-flabbergast.yaml; done

install-data-local:
	for each in $(GROUPS); do install -m 644 -D $${each}.vim $(DESTDIR)$(datadir)/vim/addons/$$each/flabbergast.vim; done

CLEANFILES = vim-syntax-flabbergast.yaml
