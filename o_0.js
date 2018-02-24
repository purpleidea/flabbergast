/* exported expandAll pageLoad searchChange searchClear showDef showHide showUse toggleSelection */
"use strict";

const validLookup = /^([a-z][a-zA-Z0-9_]*(\.[a-z][a-zA-Z0-9_]*)*)?$/;

function checkNoMatches() {
    const searchlist = document.getElementById("terms").getElementsByTagName("a");
    let any_visible = false;
    for (let it = 0; it < searchlist.length; it++) {
        if (window.getComputedStyle(searchlist[it]).getPropertyValue("display") != "none") {
            any_visible = true;
            break;
        }
    }
    document.getElementById("nomatches").style.display = any_visible ? "none" : "block";
}

function cssForArray(arr, css) {
    if (arr.length == 0) {
        return "";
    }
    return arr.map(sel => `${sel}{${css}}`).join("\n");
}

function expandAll(id) {
    for (let target = document.getElementById(id); target != null && target.nodeName == "DT"; target = target.parentElement.parentElement.previousElementSibling) {
        if (target.className == "hidden") {
            target.className = null;
        }
    }
    return true;
}

function pageLoad() {
    updateSelection("showPartials");
    updateSelection("hideExternals");
    openTab("searchtab");
    const showTerm = () => {
        const term = document.location.hash;
        if (term.startsWith("#term-")) {
            term = term.substring(6);
            const searchbox = document.getElementById("search");
            searchbox.value = term;
            searchChange();
        } else if (term.startsWith("#item-")) {
            expandAll(term.substring(1));
            // Bug: https://bugzilla.mozilla.org/show_bug.cgi?id=645075
            if (navigator.userAgent.indexOf("Firefox") > -1) {
                location.href += "";
            }
        }
    };
    const showLoadError = (context, message) => {
        const p = document.createElement("p");
        p.appendChild(document.createTextNode(`${context}: ${message || "Unknown error"}`));
        const refsTab = document.getElementById("refstab");
        refsTab.insertBefore(p, refsTab.firstChild);
    };
    const libraryNames = getLibraries();
    if (libraryNames.length == 0) {
        showTerm();
        return;
    }
    const makeLibraryInfo = name => {
        return {
            "name": name,
            "nameParts": name.split("-")
        };
    };
    const libraries = libraryNames.map(makeLibraryInfo);
    let inflight = libraries.length + 1;
    const unref = () => {
        if (--inflight > 0) {
            return;
        }
        const searchlistdiv = document.getElementById("terms");
        const sortedLibraries = libraries.sort((a, b) => {
            let result = 0;
            for (let i = 0; result == 0 && i < a.nameParts.length && i < b.nameParts.length; i++) {
                result = a.nameParts[i].localeCompare(b.nameParts[i]);
            }
            return result || (a.nameParts.length - b.nameParts.length);
        });
        sortedLibraries.filter(library => library.links).forEach(library => searchlistdiv.appendChild(library.links));
        sortedLibraries.filter(library => library.error).forEach(library => {
            const link = document.getElementById(`lib-${library.name}`);
            if (link) {
                link.className = "error";
            }
            showLoadError(library.name.replace("-", "/"), library.error);
        });
        showTerm();
    };

    const termsForExternal = new XSLTProcessor();
    termsForExternal.setParameter(null, "knownterms", getTerms().map(t => `[${t}]`).join(""));
    const downloadLibrary = info => {
        const request = new XMLHttpRequest();
        request.addEventListener("error", () => {
            unref();
            info.error = request.statusText || "Unknown error";
        });
        request.addEventListener("load", () => {
            info.links = termsForExternal.transformToFragment(request.responseXML, document);
            const nsResolver = request.responseXML.createNSResolver(request.responseXML.documentElement);
            const newLibraries = request.responseXML.evaluate("//o_0:ref/text()[not(contains(., 'interop'))]", request.responseXML.documentElement, nsResolver, XPathResult.UNORDERED_NODE_ITERATOR_TYPE, null);
            for (let newNameNode = newLibraries.iterateNext(); newNameNode; newNameNode = newLibraries.iterateNext()) {
                const newName = newNameNode.textContent.replace("/", "-");
                if (libraries.every(existing => existing.name != newName)) {
                    const newInfo = makeLibraryInfo(newName);
                    libraries.push(newInfo);
                    inflight++;
                    downloadLibrary(newInfo);
                }
            }
            unref();
        });
        request.open("GET", `doc-${info.name}.xml`, true);
        request.send();
    };
    const xsltRequest = new XMLHttpRequest();
    xsltRequest.addEventListener("error", () => {
        showTerm();
        showLoadError("External references", xsltRequest.statusText);
    });
    xsltRequest.addEventListener("load", () => {
        try {
            termsForExternal.importStylesheet(xsltRequest.responseXML);
        } catch (e) {
            showLoadError("External references", e);
            showTerm();
            return;
        }
        libraries.forEach(library => {
            try {
                downloadLibrary(library);
            } catch (e) {
                library.error = e;
                unref();
            }
        });
        unref();
    });
    xsltRequest.open("GET", "o_0-xref.xsl", true);
    xsltRequest.overrideMimeType("text/xml");
    xsltRequest.send();
}

function openTab(tab) {
    let i;
    const tabs = document.getElementsByClassName("tab");
    for (i = 0; i < tabs.length; i++) {
        tabs[i].style.display = "none";
    }
    const buttons = document.getElementById("tabbar").children;
    for (i = 0; i < buttons.length; i++) {
        buttons[i].className = "";
    }
    document.getElementById(tab).style.display = null;
    document.getElementById(`${tab}button`).className = "selected";
}

function searchClear() {
    const searchbox = document.getElementById("search");
    searchbox.value = "";
    searchbox.focus();
    searchChange();
}

function searchChange() {
    const searchbox = document.getElementById("search");
    const termcss = document.getElementById("termcss");
    if (searchbox.value.length == 0) {
        searchbox.className = null;
        termcss.innerHTML = "";
        updateRefs(null);
    } else if (validLookup.test(searchbox.value)) {
        const searchterm = searchbox.value.replace(".", "-");
        updateRefs(searchterm);
        searchbox.className = null;
        const exact_defs = [];
        const exact_uses = [];
        const starts_defs = [];
        const starts_uses = [];
        const contains_defs = [];
        const contains_uses = [];
        const unmatched = ["#terms a.defnone", "#terms a.usenone"];

        for (let known_term of getTerms()) {
            const def_sel = `#terms a.def_${known_term}`;
            const use_sel = `#terms a.use_${known_term}`;
            if (known_term == searchterm) {
                exact_defs.push(def_sel);
                exact_uses.push(use_sel);
            } else if (known_term.startsWith(searchterm)) {
                starts_defs.push(def_sel);
                starts_uses.push(use_sel);
            } else if (known_term.indexOf(searchterm) != -1) {
                contains_defs.push(def_sel);
                contains_uses.push(use_sel);
            } else {
                unmatched.push(def_sel);
                unmatched.push(use_sel);
            }
        }

        let cssParts;
        if (exact_defs.length > 0 || exact_uses.length > 0 || starts_defs.length > 0 || starts_uses.length > 0) {
            const showPartials = getSelection("showPartials") || (exact_defs.length == 0 && exact_uses.length == 0);
            cssParts = [{
                    items: exact_defs,
                    css: "font-weight: bold; color: #4F94CD; display: block !important;"
                },
                {
                    items: exact_uses,
                    css: "font-weight: bold; display: block !important;"
                },
                {
                    items: starts_defs,
                    css: showPartials ? "color: #4F94CD; display: block !important;" : "display: none;"
                },
                {
                    items: starts_uses,
                    css: showPartials ? "display: block !important;" : "display: none;"
                },
                {
                    items: unmatched.concat(contains_defs, contains_uses),
                    css: "display: none;"
                }
            ];
        } else {
            cssParts = [{
                    items: contains_defs,
                    css: "font-weight: bold; color: #4F94CD; display: block !important;"
                },
                {
                    items: contains_uses,
                    css: "font-weight: bold; display: block !important;"
                },
                {
                    items: unmatched,
                    css: "display: none;"
                }
            ];
        }

        termcss.innerHTML = cssParts.map(part => cssForArray(part.items, part.css)).join("") + (getSelection("hideExternals") ? "\n#terms a.external { display: none !important; }" : "");
        checkNoMatches();
    } else {
        searchbox.className = "error";
    }
    return true;
}

function showDef(term) {
    showTerm(term, "def");
}

function showUse(term) {
    showTerm(term, "use");
}

function showTerm(term, prefix) {
    const termcss = document.getElementById("termcss");
    const visible = [];
    const hidden = [`#terms a.${prefix}none`];
    for (let known_term of getTerms()) {
        (known_term == term ? visible : hidden).push(`#terms a.${prefix}_${known_term}`);
    }
    termcss.innerHTML = cssForArray(visible, (prefix == "def" ? "color: #4F94CD; " : "") + "font-weight: bold; display: block !important;") + cssForArray(hidden, "display: none;");
    checkNoMatches();
}

function showHide(roller) {
    const is_hidden = roller.parentNode.parentNode.className == "hidden";
    roller.parentNode.parentNode.className = is_hidden ? "" : "hidden";
}

function getSelection(name) {
    return (window.localStorage.getItem(name) || "false") === "true";
}

function updateSelection(name) {
    const value = (window.localStorage.getItem(name) || "true") === "true";
    document.getElementById(name).className = value ? "option-on" : "option-off";
}

function toggleSelection(element) {
    const value = (window.localStorage.getItem(element.id) || "true") !== "true";
    element.className = value ? "option-on" : "option-off";
    window.localStorage.setItem(element.id, value ? "true" : "false");
    searchChange();
}

function updateRefs(term) {
    const hash_name = term == null ? null : (`term-${term}`);
    const reflist = document.getElementById("references").childNodes;
    for (let it = 0; it < reflist.length; it++) {
        if (reflist[it].tagName == "A") {
            reflist[it].hash = hash_name;
        }
    }
}
