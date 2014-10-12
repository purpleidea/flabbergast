" Vim syntax file
" Language:	Flabbergast
" Maintainer:	Andre Masella <andre@masella.name>
" Filenames:	*.flbgst

" Quit when a syntax file was already loaded
if exists("b:current_syntax")
    finish
endif

let s:flabbergast_cpo_save = &cpo
set cpo&vim

syn case match

syn region flabbergastString start='"' end='"' contains=flabbergastEscape,flabbergastEscapeError,@Spell
syn match flabbergastEscapeError contained "\\." display
syn match flabbergastEscape contained '\\[abfnvrt"\\]' display
syn match flabbergastEscape contained "\\\(\o\{3}\|x\x\{2}\|u\x\{4}\)" display
syn region flabbergastEscape contained start="\\(" end=")" contains=TOP

syn match flabbergastTrailingWhite "[ \t]\+$"
syn match flabbergastTrailingWhite "[ \t]\+$" containedin=ALL
syn match flabbergastAttribute "[+?-]\?:" display
syn match flabbergastComment "#.*$" contains=flabbergastTodo,@Spell display
syn keyword flabbergastConstant Continue False FloatMax FloatMin Infinity IntMax IntMin NaN Null True
syn match flabbergastIdentifier "\<args\>" display
syn match flabbergastIdentifier "\<value\>" display
syn match flabbergastBadKeyword "\<[A-WYZ][^ \t#]*" display
syn match flabbergastIdentifierString "\$[a-z][a-zA-Z0-9_]*\>" display
syn match flabbergastConstant "[A-Za-z0-9.+-]\+:[A-Za-z0-9~!*'();@&=+$,/?%#.+-]\+" display
syn keyword flabbergastKeyword As By Container Each Error Finite For From GenerateId Id If Then Else In Is Length Let Lookup Name Order Ordinal Reduce Select Template This Through To Where With 
syn match flabbergastOperators '\(<=\?\|<\=>\|>=\?\|==\|||\|-\|!\|!=\|/\|\*\|&\|&&\|%\|+\)' display
syn keyword flabbergastTodo contained containedin=flabbergastComment TODO FIXME XXX
syn keyword flabbergastType Bool Float Int Str Tuple

hi def link flabbergastComment		Comment
hi def link flabbergastConstant		Constant
hi def link flabbergastEscapeError	Error
hi def link flabbergastBadKeyword	Error
hi def link flabbergastIdentifier	Identifier
hi def link flabbergastEscape		SpecialChar
hi def link flabbergastKeyword		Statement
hi def link flabbergastOperators	Operator
hi def link flabbergastAttribute	Statement
hi def link flabbergastIdentifierString	String
hi def link flabbergastString		String
hi def link flabbergastTodo		Todo
hi def link flabbergastTrailingWhite	DiffDelete
hi def link flabbergastType		Type

let b:current_syntax = "flabbergast"

let &cpo = s:flabbergast_cpo_save
unlet s:flabbergast_cpo_save

" vim: ts=8
