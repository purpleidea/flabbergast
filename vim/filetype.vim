if exists("b:did_ftplugin") | finish | endif
let b:did_ftplugin = 1
let s:save_cpo = &cpo
set cpo&vim

setlocal cinkeys-=0#
setlocal indentkeys-=0#
setlocal include=^\\s*\\(from\\\|import\\)
setlocal includeexpr=substitute(v:fname,'\\.','/','g')
setlocal suffixesadd=.py
setlocal comments-=:%
setlocal commentstring=#%s

setlocal noexpandtab tabstop=2

setlocal comments=:#
setlocal commentstring=#%s

let &cpo = s:save_cpo
unlet s:save_cpo
