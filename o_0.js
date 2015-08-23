var validLookup = /^([a-z][a-zA-Z0-9_]*(\.[a-z][a-zA-Z0-9_]*)*)?$/;

function cssForArray(arr, css) {
    if (arr.length == 0) {
        return "";
    }
    return arr.join() + "{" + css + "}";
}

function pageLoad() {
    var term = document.location.hash;
    if (term.length > 0 && term.startsWith("term-")) {
        term = term.substring(5);
        var searchbox = document.getElementById('search');
        searchbox.value = term;
        searchChange();
    }
}

function searchClear(clear_element) {
    var searchbox = clear_element.previousSibling;
    searchbox.value = '';
    searchbox.focus();
    searchChange();
}

function searchChange() {
    var searchbox = document.getElementById('search');
    var termcss = document.getElementById('termcss');
    if (searchbox.value.length == 0) {
        searchbox.className = null;
        termcss.innerHTML = "";
        updateRefs(null);
    } else if (searchbox.value.match(validLookup)) {
        var searchterm = searchbox.value.replace(".", "-");
        updateRefs(searchterm);
        searchbox.className = null;
        var exact_defs = [];
        var exact_uses = [];
        var starts_defs = [];
        var starts_uses = [];
        var contains_defs = [];
        var contains_uses = [];
        var unmatched = [];

        var known_terms = getTerms();
        for (var it = 0; it < known_terms.length; it++) {
            var def_sel = "#terms a.def_" + known_terms[it];
            var use_sel = "#terms a.use_" + known_terms[it];
            if (known_terms[it] == searchterm) {
                exact_defs.push(def_sel);
                exact_uses.push(use_sel);
            } else if (known_terms[it].startsWith(searchterm)) {
                starts_defs.push(def_sel);
                starts_uses.push(use_sel);
            } else if (known_terms[it].indexOf(searchterm) != -1) {
                contains_defs.push(def_sel);
                contains_uses.push(use_sel);
            } else {
                unmatched.push(def_sel);
                unmatched.push(use_sel);
            }
        }

        if (exact_defs.length > 0 || exact_uses.length > 0 || starts_defs.length > 0 || starts_defs.length > 0) {
            termcss.innerHTML = cssForArray(exact_defs, "font-weight: bold; color: #4F94CD; display: block !important;") + cssForArray(exact_uses, "font-weight: bold; display: block !important;") + cssForArray(starts_defs, "color: #4F94CD; display: block !important;") + cssForArray(starts_uses, "display: block !important;") + cssForArray(unmatched.concat(contains_defs, contains_uses), "display: none;");
        } else {
            termcss.innerHTML = cssForArray(contains_defs, "font-weight: bold; color: #4F94CD; display: block !important;") + cssForArray(contains_uses, "font-weight: bold; display: block !important;") + cssForArray(unmatched, "display: none;");
        }
    } else {
        searchbox.className = "error";
    }
    return true;
}

function showDef(term) {
    showTerm(term, 'def');
}

function showUse(term) {
    showTerm(term, 'use');
}

function showTerm(term, prefix) {
    var termcss = document.getElementById('termcss');
    var visible = [];
    var hidden = [];
    var known_terms = getTerms();
    for (var it = 0; it < known_terms.length; it++) {
        (known_terms[it] == term ? visible : hidden).push("#terms a." + prefix + "_" + known_terms[it]);
    }
    termcss.innerHTML = cssForArray(visible, (prefix == "def" ? "color: #4F94CD; " : "") + "font-weight: bold; display: block !important;") + cssForArray(hidden, "display: none;");
}

function showHide(roller) {
    var is_hidden = roller.parentNode.className == "hidden";
    roller.textContent = is_hidden ? "▼" : "▶";
    roller.parentNode.className = is_hidden ? "" : "hidden";
}

function updateRefs(term) {
    var hash_name = term == null ? null : ("term-" + term);
    var reflist = document.getElementById('references').childNodes;
    for (var it = 0; it < reflist.length; it++) {
        reflist[it].href.hash = hash_name;
    }
}