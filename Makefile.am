ACLOCAL_AMFLAGS = -I m4
NULL =

SUBDIRS = \
	platform/jvm \
	vim \
	$(NULL)

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
	$(NULL)

mime_DATA = flabbergast.xml

pkgdata_DATA = \
	o_0.css \
	o_0.js \
	o_0.xsl \
	o_0-xref.xsl \
	$(NULL)

clean-local:
	rm -rf pkgroot
