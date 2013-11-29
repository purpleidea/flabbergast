ACLOCAL_AMFLAGS = -I m4
NULL =

CLEANFILES = \
	flabbergast.h \
	$(PACKAGE_NAME)-$(PACKAGE_MAJOR).pc \
	$(PACKAGE_NAME)-$(PACKAGE_MAJOR).vapi \
	vala-doc \
	$(NULL)

DISTCLEANFILES = \
	$(NULL)

EXTRA_DIST = \
	lib.deps.in \
	lib.pc.in \
	$(libflabbergast_glib_la_SOURCES) \
	$(NULL)

lib_LTLIBRARIES = libflabbergast-glib.la

pkginclude_HEADERS = flabbergast.h

pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA = $(PACKAGE_NAME)-$(PACKAGE_MAJOR).pc

vapidir = $(datadir)/vala/vapi
dist_vapi_DATA = $(PACKAGE_NAME)-$(PACKAGE_MAJOR).deps $(PACKAGE_NAME)-$(PACKAGE_MAJOR).vapi

girdir = $(datadir)/gir-1.0
dist_gir_DATA = $(PACKAGE_NAME)-$(PACKAGE_MAJOR).gir

libflabbergast_glib_la_COMMONVALAFLAGS =\
	--pkg gee-0.8 \
	--vapidir=$(top_srcdir) \
	$(NULL)

libflabbergast_glib_la_VALAFLAGS = \
	-g \
	--gir $(PACKAGE_NAME)-$(PACKAGE_MAJOR).gir \
	-H flabbergast.h \
	--library=$(PACKAGE_NAME)-$(PACKAGE_MAJOR) \
	$(libflabbergast_glib_la_COMMONVALAFLAGS)

libflabbergast_glib_la_CPPFLAGS = -g \
	$(GLIB_CFLAGS) \
	$(GEE_CFLAGS) \
	$(NULL)

libflabbergast_glib_la_LIBADD = \
	$(GLIB_LIBS) \
	$(GEE_LIBS) \
	$(NULL)

libflabbergast_glib_la_SOURCES = \
	engine.vala \
	expr.vala \
	data.vala \
	utils.vala \
	$(NULL)

if ENABLE_VALADOC
VALADOCFLAGS = --package-name=$(PACKAGE_NAME)-$(PACKAGE_MAJOR) -o doc --vapidir=$(top_srcdir)
vala-doc: $(libflabbergast_glib_la_SOURCES)
	test ! -d doc || rm -r doc
	$(VALADOC) $(VALADOCFLAGS) $(libflabbergast_glib_la_COMMONVALAFLAGS) $^ && touch vala-doc
endif

