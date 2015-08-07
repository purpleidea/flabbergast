ACLOCAL_AMFLAGS = -I m4
NULL =

SUBDIRS = \
	vim \
	$(NULL)

if BUILD_CLI
SUBDIRS += platform/cli
endif

if BUILD_JVM
SUBDIRS += platform/jvm
endif

mimedir = $(datadir)/mime/packages

bin_SCRIPTS = \
	update-flabbergast \
	$(NULL)

man1_MANS = \
	update-flabbergast.1 \
	$(NULL)

man7_MANS = \
	flabbergast_language.7 \
	flabbergast_sql.7 \
	$(NULL)

mime_DATA = flabbergast.xml

install-data-hook:
	@for f in $$(find lib -name '*.o_0' -o -name '*.?o_0' -type f); do \
		$(mkinstalldirs) $(DESTDIR)$(pkgdatadir)/$$(dirname $$f); \
		$(INSTALL_DATA) $(srcdir)/$$f $(DESTDIR)$(pkgdatadir)/$$f; \
	done	
