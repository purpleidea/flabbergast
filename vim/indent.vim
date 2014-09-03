" Vim indent file
" Language: Flabbergast
" Maintainer: Andre Masella <andre@masella.name>
" Latest Revision: 2014-05-03

if exists("b:did_indent")
	finish
endif
let b:did_indent = 1

setlocal nolisp
setlocal nosmartindent
setlocal autoindent
setlocal indentexpr=GetFlabbergastIndent(v:lnum)
setlocal indentkeys=0{,0},!^F,o,O

let b:undo_indent = "setl smartindent< indentkeys< indentexpr<"

if exists("*GetFlabbergastIndent")
	finish
endif

function IsValidFlabbergastMatch()
	return synIDattr(synID(line('.'), col('.'), 1), 'name') =~ '\%(Comment\|String\)$'
endfunction

function! GetFlabbergastIndent(lnum)
	let openmatch = '\%(\<\%(For\|Let\|If\)\>\|(\|{\|\[\)'
	let middlematch = '\<\%(Then\|In\|Reduce\)\>'
	let closematch = '\%(\<\%(Else\|Select\|With\)\>\|)\|}\|\]\)'
	" Multi-line strings should not be indented
	if synIDattr(synID(a:lnum, 1, 1), 'name') =~ 'String$'
		return -1
	endif

	let save_cursor = getpos(".")

	" Find the previous line which is not a comment or string
	let plnum = prevnonblank(a:lnum - 1)
	while synIDattr(synID(plnum, 1, 1), 'name') =~ '\(String|Comment\)$' && plnum > 0
		let plnum = prevnonblank(plnum - 1)
	endwhile

	" Beginning of file -- no indentation
	if plnum == 0
		return 0
	endif

	" If this is a hanging attribute definition, indent.
	let i = 0
	if getline(plnum) =~ '^[^#]*[^#?%-]:\s*\(#.*\)?'
		let i = 1
	endif

	" Determine if there is an open expression
	call cursor(a:lnum, 1)
	let i += searchpair(openmatch, middlematch, closematch, 'mWrb', "IsValidFlabbergastMatch()", plnum)
 
	" Check closed keywords on current line
	call cursor(plnum, col([plnum,'$']))
	let i -= searchpair(openmatch, middlematch, closematch, 'mWr', "IsValidFlabbergastMatch()", a:lnum)
  
	" Restore cursor
	call setpos(".", save_cursor)

	return indent(plnum) + i * &sw
endfunction
