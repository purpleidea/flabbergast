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

bin_PROGRAMS = flabbergast

lib_LTLIBRARIES = libflabbergast-glib.la

pkginclude_HEADERS = flabbergast.h

pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA = $(PACKAGE_NAME)-$(PACKAGE_MAJOR).pc

vapidir = $(datadir)/vala/vapi
dist_vapi_DATA = $(PACKAGE_NAME)-$(PACKAGE_MAJOR).deps $(PACKAGE_NAME)-$(PACKAGE_MAJOR).vapi

girdir = $(datadir)/gir-1.0
dist_gir_DATA = $(PACKAGE_NAME)-$(PACKAGE_MAJOR).gir

flabbergast_VALAFLAGS = \
	-g \
	--pkg $(PACKAGE_NAME)-$(PACKAGE_MAJOR) \
	--vapidir=$(top_srcdir) \
	$(NULL)

flabbergast_CPPFLAGS = -g \
	-I. \
	$(GLIB_CFLAGS) \
	$(GEE_CFLAGS) \
	$(GTEONOMA_CFLAGS) \
	$(NULL)
	
flabbergast_LDADD = \
	libflabbergast-glib.la
	$(GLIB_LIBS) \
	$(GEE_LIBS) \
	$(GTEONOMA_LIBS) \
	$(NULL)

flabbergast_SOURCES = \
	main.vala \
	$(NULL)

libflabbergast_glib_la_COMMONVALAFLAGS =\
	--pkg gee-0.8 \
	--pkg libgteonoma-1 \
	--pkg util \
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
	$(GTEONOMA_CFLAGS) \
	$(NULL)

libflabbergast_glib_la_LIBADD = \
	$(GLIB_LIBS) \
	$(GEE_LIBS) \
	$(GTEONOMA_LIBS) \
	$(NULL)

libflabbergast_glib_la_SOURCES = \
	data.vala \
	engine.vala \
	expr.vala \
	expr_arith.vala \
	expr_fricassee.vala \
	expr_misc.vala \
	expr_tuple.vala \
	expr_type.vala \
	parser.vala \
	utils.vala \
	$(NULL)

if ENABLE_VALADOC
VALADOCFLAGS = --package-name=$(PACKAGE_NAME)-$(PACKAGE_MAJOR) -o doc --vapidir=$(top_srcdir)
vala-doc: $(libflabbergast_glib_la_SOURCES)
	test ! -d doc || rm -r doc
	$(VALADOC) $(VALADOCFLAGS) $(libflabbergast_glib_la_COMMONVALAFLAGS) $^ && touch vala-doc
endif

