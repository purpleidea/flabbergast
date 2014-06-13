EXT = $(notdir $(HANDLER))
SOURCES = $(shell find . -type f -name '*.flbgst')
TARGETS = $(patsubst %.flbgst, %.$(EXT), $(SOURCES))
CACHES = $(shell find . -type f -name '*.$(EXT)')

.PHONY: all
all: $(TARGETS)
	@echo rm -f $(filter-out $(TARGETS), $(CACHES))

%.$(EXT): %.flbgst
	@$(HANDLER) $<
