EXTS =
ALL = $(shell find . -type f)
SOURCES = $(filter %.flbgst, $(ALL))
TARGETS = $(foreach EXT, $(EXTS), $(patsubst %.flbgst, %.$(EXT), $(SOURCES)))

.PHONY: all
all: $(TARGETS)
	@echo rm -f $(filter-out $(SOURCES) $(TARGETS), $(ALL))

include: $(wildcard $(libdir)/flabbergast-compiler/*)
