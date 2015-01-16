GROUPS = indent ftdetect syntax

all: vim-syntax-flabbergast.yaml

vim-syntax-flabbergast.yaml: Makefile
	echo addon: flabbergast-syntax > vim-syntax-flabbergast.yaml
	echo description: "Addon to highlight Flabbergast" >> vim-syntax-flabbergast.yaml
	echo files: >> vim-syntax-flabbergast.yaml
	for each in $(GROUPS); do echo " - $${each}/flabbergast.vim" >> vim-syntax-flabbergast.yaml; done

install:
	for each in $(GROUPS); do install -m 644 -D $${each}.vim $(DESTDIR)/usr/share/vim/addons/$$each/flabbergast.vim; done
	install -m 644 -D vim-syntax-flabbergast.yaml $(DESTDIR)/usr/share/vim/registry/vim-syntax-flabbergast.yaml

clean:
	rm -f vim-syntax-flabbergast.yaml

.PHONY: all install clean
