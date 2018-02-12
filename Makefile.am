ACLOCAL_AMFLAGS = -I m4
NULL =

SUBDIRS = \
	vim \
	$(NULL)

if BUILD_JVM
SUBDIRS += platform/jvm
endif

mimedir = $(datadir)/mime/packages

bin_SCRIPTS = \
	flabbergast-diff \
	update-flabbergast \
	$(NULL)

man1_MANS = \
	flabbergast-diff.1 \
	update-flabbergast.1 \
	$(NULL)

man7_MANS = \
	flabbergast_apidoc.7 \
	flabbergast_language.7 \
	flabbergast_uri.7 \
	flabbergast_sql.7 \
	$(NULL)

mime_DATA = flabbergast.xml

pkgdata_DATA = \
	o_0.css \
	o_0.js \
	o_0.xsl \
	o_0-xref.xsl \
	$(NULL)

PERCENT = %

install-data-hook:
	for f in $$(find stdlib \( -name '*.o_0' -o -name '*.?o_0' \) -type f -printf "$(PERCENT)P " ); do \
		$(mkinstalldirs) $$(dirname $(DESTDIR)$(pkgdatadir)/lib/$${f}); \
		$(INSTALL_DATA) $(srcdir)/stdlib/$$f $(DESTDIR)$(pkgdatadir)/lib/$${f}; \
	done	

clean-local:
	rm -rf pkgroot
