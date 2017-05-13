package flabbergast.interop;

import flabbergast.lang.Name;

interface EmitDefineBlock {
  EmitBlock bind(Name functionName, Name blockName);
}
