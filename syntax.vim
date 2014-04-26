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

syn match flabbergastAttribute "[+?-]\?:" display
syn match flabbergastComment "#.*$" contains=flabbergastTodo,@Spell display
syn keyword flabbergastConstant False Null True
syn match flabbergastIdentifier "\<[a-z][a-zA-Z0-9_]*\>" display
syn match flabbergastIdentifierString "\$[a-z][a-zA-Z0-9_]*\>" display
syn keyword flabbergastKeyword As By Container Error Finite For From If Then Else In Is Let Lookup NaN Order Ordinal Reduce Select Template This Through To Where With 
syn match flabbergastOperators '\(<=\?\|<\=>\|>=\?\|==\|||\|-\|!\|!=\|/\|\*\|&\|&&\|%\|+\)' display
syn keyword flabbergastTodo contained TODO FIXME XXX
syn keyword flabbergastType Bool Float Int Str Tuple

hi def link flabbergastComment		Comment
hi def link flabbergastConstant		Constant
hi def link flabbergastEscapeError	Error
hi def link flabbergastIdentifier	Identifier
hi def link flabbergastEscape		SpecialChar
hi def link flabbergastKeyword		Statement
hi def link flabbergastOperators	Operator
hi def link flabbergastAttribute	Statement
hi def link flabbergastIdentifierString	String
hi def link flabbergastString		String
hi def link flabbergastTodo		Todo
hi def link flabbergastType		Type

let b:current_syntax = "flabbergast"

let &cpo = s:flabbergast_cpo_save
unlet s:flabbergast_cpo_save

" vim: ts=8
