// Generated from grammar/alloy.tmLanguage.json by codemirror/build.py.
// This is a CodeMirror rule table, not an embedded TextMate grammar.
import {StreamLanguage} from "@codemirror/language";
import {tags} from "@lezer/highlight";

const NO_MATCH = Symbol("no match");
const RESTART = Symbol("restart after context entry");
function scanNothing() { return NO_MATCH; }
function scanComments(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule1" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("//", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule1"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "$", endCaptures: {}, style: "comment", contentStyle: null, scan: scanNothing, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "comment"}, "comment") : RESTART;
  }
}
  if (state.zeroRule !== "rule2" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("--", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule2"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "$", endCaptures: {}, style: "comment", contentStyle: null, scan: scanNothing, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "comment"}, "comment") : RESTART;
  }
}
  if (state.zeroRule !== "rule3" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("/\\*\\*(?!/)", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule3"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "\\*/", endCaptures: {"0": "comment"}, style: "docComment", contentStyle: null, scan: scanNothing, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "comment"}, "docComment") : RESTART;
  }
}
  if (state.zeroRule !== "rule4" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("/\\*", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule4"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "\\*/", endCaptures: {"0": "comment"}, style: "comment", contentStyle: null, scan: scanNothing, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "comment"}, "comment") : RESTART;
  }
}
  return NO_MATCH;
}

function scanMetaString(stream, state) {
  let m, result;
  if ((m = matchAt("\\\\.", stream, state))) return emitMatch(stream, state, m, {}, "string");
  return NO_MATCH;
}

function scanStrings(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule5" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("\"", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule5"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "\"", endCaptures: {"0": "string"}, style: null, contentStyle: "string", scan: scanMetaString, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "string"}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanDeclarationKeywords(stream, state) {
  let m, result;
  if ((m = matchAt("(?:\\b(pred|fun)\\b)|(?:\\b(enum)\\b)|(?:\\b(module)\\b)|(?:\\b(run|check)\\b)|(?:\\b(assert)\\b)|(?:\\b(fact)\\b)", stream, state))) return emitMatch(stream, state, m, {}, "keyword");
  return NO_MATCH;
}

function scanContext(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if ((result = scanStrings(stream, state)) !== NO_MATCH) return result;
  if ((result = scanTopLevelLetHeader(stream, state)) !== NO_MATCH) return result;
  if ((result = scanTopLevelLetExpressionBody(stream, state)) !== NO_MATCH) return result;
  if ((result = scanTopLevelLetBlockBody(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanTopLevelLetDeclarations(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule6" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("\\blet\\b", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule6"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "(?<=\\})|(?=(?:[ \\t]+|/\\*[\\s\\S]*?\\*/|//[^\\r\\n]*|--[^\\r\\n]*)*(?:(?:private|abstract|one|lone|some|var)\\b[ \\t]+)*(?:module|open|sig|enum|pred|fun|fact|assert|run|check|let)\\b)", endCaptures: {}, style: null, contentStyle: null, scan: scanContext, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "keyword"}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanContext8(stream, state) {
  let m, result;
  if ((result = scanIdentifierListItems(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanContext7(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if (state.zeroRule !== "rule8" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("\\[", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule8"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "\\]", endCaptures: {"0": "punctuation"}, style: null, contentStyle: null, scan: scanContext8, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "punctuation"}, null) : RESTART;
  }
}
  if ((result = scanIdentifiers(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanTopLevelLetHeader(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule7" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("(?=[A-Za-z_])", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule7"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "(?=(?:\\s|//[^\\r\\n]*|--[^\\r\\n]*|/\\*[\\s\\S]*?\\*/)*(?:=|\\{))", endCaptures: {}, style: null, contentStyle: null, scan: scanContext7, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanMetaBindingMacro(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if ((result = scanStrings(stream, state)) !== NO_MATCH) return result;
  if ((result = scanBlockExpressions(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanTopLevelLetExpressionBody(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule9" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("=", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule9"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "(?=(?:[ \\t]+|/\\*[\\s\\S]*?\\*/|//[^\\r\\n]*|--[^\\r\\n]*)*(?:(?:private|abstract|one|lone|some|var)\\b[ \\t]+)*(?:module|open|sig|enum|pred|fun|fact|assert|run|check|let)\\b)", endCaptures: {}, style: null, contentStyle: null, scan: scanMetaBindingMacro, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "operator"}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanTopLevelLetBlockBody(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule10" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("(?=\\{)", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule10"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "(?<=\\})", endCaptures: {}, style: null, contentStyle: null, scan: scanMetaBindingMacro10, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanContext11(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if ((result = scanStrings(stream, state)) !== NO_MATCH) return result;
  if ((result = scanCallableParameterValue(stream, state)) !== NO_MATCH) return result;
  if ((m = matchAt("\\b(disj|var)\\b(?!\\$)", stream, state))) return emitMatch(stream, state, m, {}, "keyword");
  if ((m = matchAt(",", stream, state))) return emitMatch(stream, state, m, {}, "punctuation");
  if ((result = scanBlockExpressions(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanCallableDeclarationParameters(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule11" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("\\b(fun|pred)\\b\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(\\[)", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule11"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "\\]", endCaptures: {"0": "punctuation"}, style: null, contentStyle: null, scan: scanContext11, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"1": "keyword", "2": "variableName", "3": "punctuation"}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanIdentifierListItems(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if ((m = matchAt(",", stream, state))) return emitMatch(stream, state, m, {}, "punctuation");
  if ((result = scanIdentifiers(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanModulePathArguments(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule12" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("\\[", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule12"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "\\]", endCaptures: {"0": "punctuation"}, style: null, contentStyle: null, scan: scanMetaParametersModule, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "punctuation"}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanModulePathComponents(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if ((result = scanModulePathArguments(stream, state)) !== NO_MATCH) return result;
  if ((m = matchAt("\\b[A-Za-z_][A-Za-z0-9_]*\\b", stream, state))) return emitMatch(stream, state, m, {}, "variableName");
  if ((m = matchAt("/", stream, state))) return emitMatch(stream, state, m, {}, "punctuation");
  return NO_MATCH;
}

function scanMetaPathModule(stream, state) {
  let m, result;
  if ((result = scanModulePathComponents(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanMetaPathOpen(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if ((m = matchAt("\\bas\\b", stream, state))) return emitMatch(stream, state, m, {}, "keyword");
  if ((result = scanModulePathComponents(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanModuleDeclarationsAndImports(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule13" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("\\b(module)\\b", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule13"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "(?=(?:\\s|//[^\\r\\n]*|--[^\\r\\n]*|/\\*[\\s\\S]*?\\*/)*(?:open|sig|enum|pred|fun|fact|assert|run|check|let|private|abstract|one|lone|some|var)\\b)", endCaptures: {}, style: null, contentStyle: null, scan: scanMetaPathModule, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "keyword"}, null) : RESTART;
  }
}
  if (state.zeroRule !== "rule14" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("\\b(open)\\b", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule14"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "(?=(?:\\s|//[^\\r\\n]*|--[^\\r\\n]*|/\\*[\\s\\S]*?\\*/)*(?:as\\s+[A-Za-z_][A-Za-z0-9_]*\\s*)?(?:open|sig|enum|pred|fun|fact|assert|run|check|let|private|abstract|one|lone|some|var)\\b)", endCaptures: {}, style: null, contentStyle: null, scan: scanMetaPathOpen, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "keyword"}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanContext15(stream, state) {
  let m, result;
  if ((m = matchAt("\\b(sig)\\b(?!\\$)", stream, state))) return emitMatch(stream, state, m, {}, "keyword");
  if ((result = scanSignatureHierarchy(stream, state)) !== NO_MATCH) return result;
  if ((result = scanSignatureFields(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanSignatureDeclarations(stream, state) {
  let m, result;
  if ((m = matchAt("(?:\\b(abstract|private|var)\\b(?!\\$))|(?:\\b(one|lone|some)\\b(?=(?:(?:\\s+)|/\\*[\\s\\S]*?\\*/|//[^\\r\\n]*|--[^\\r\\n]*|\\b(?:abstract|private|var|one|lone|some)\\b)*\\bsig\\b(?!\\$)))", stream, state))) return emitMatch(stream, state, m, {}, "keyword");
  if (state.zeroRule !== "rule15" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("(?=\\bsig\\b(?!\\$))", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule15"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "(?<=\\})", endCaptures: {}, style: null, contentStyle: null, scan: scanContext15, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanMetaIdentifiers(stream, state) {
  let m, result;
  if ((m = matchAt("(?:\\b(sig|field)\\$(?![A-Za-z0-9_]))|(?:\\b(static|var)\\$(?![A-Za-z0-9_]))", stream, state))) return emitMatch(stream, state, m, {}, "constant");
  if ((m = matchAt("\\$", stream, state))) return emitMatch(stream, state, m, {}, "punctuation");
  return NO_MATCH;
}

function scanKeywords(stream, state) {
  let m, result;
  if ((m = matchAt("(?:\\b(for|but|exactly|expect|with)\\b)|(?:\\b(enabled|event|invariant|modifies)\\b)", stream, state))) return emitMatch(stream, state, m, {}, "keyword");
  if ((m = matchAt("\\b(Int|String|int|seq|steps)\\b", stream, state))) return emitMatch(stream, state, m, {}, "typeName");
  if ((m = matchAt("\\b(this)\\b", stream, state))) return emitMatch(stream, state, m, {}, "atom");
  return NO_MATCH;
}

function scanMetaGroup(stream, state) {
  let m, result;
  if ((result = scanBlockExpressions(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanExpressionGroups(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule16" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("\\(", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule16"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "\\)", endCaptures: {"0": "punctuation"}, style: null, contentStyle: null, scan: scanMetaGroup, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "punctuation"}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanMetaBrackets(stream, state) {
  let m, result;
  if ((m = matchAt("\\b(disj)\\b(?!\\$)", stream, state))) return emitMatch(stream, state, m, {}, "keyword");
  if ((result = scanBlockExpressions(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanExpressionBrackets(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule17" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("\\[", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule17"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "\\]", endCaptures: {"0": "punctuation"}, style: null, contentStyle: null, scan: scanMetaBrackets, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "punctuation"}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanExpressionBlocks(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule18" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("\\{", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule18"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "\\}", endCaptures: {"0": "punctuation"}, style: null, contentStyle: null, scan: scanMetaBlock, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "punctuation"}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanContext19(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if ((result = scanStrings(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanContext22(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if ((result = scanStrings(stream, state)) !== NO_MATCH) return result;
  if ((result = scanIdentifiers(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanMetaBindingLet21(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if ((result = scanStrings(stream, state)) !== NO_MATCH) return result;
  if (state.zeroRule !== "rule22" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt(",", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule22"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "(=)|(?=\\S)(?![A-Za-z_]|/\\*|//|--)", endCaptures: {"1": "operator"}, style: null, contentStyle: null, scan: scanContext22, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "punctuation"}, null) : RESTART;
  }
}
  if ((result = scanBlockExpressions(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanMetaBindingLet(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule21" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("=", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule21"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "(?=\\|(?!\\|)|\\{(?!\\s*[A-Za-z_][A-Za-z0-9_]*\\s*:))", endCaptures: {}, style: null, contentStyle: null, scan: scanMetaBindingLet21, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "operator"}, null) : RESTART;
  }
}
  if ((result = scanBlockExpressions(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanBlockExpressions(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if ((result = scanStrings(stream, state)) !== NO_MATCH) return result;
  if ((result = scanExpressionGroups(stream, state)) !== NO_MATCH) return result;
  if ((result = scanExpressionBrackets(stream, state)) !== NO_MATCH) return result;
  if ((result = scanExpressionBlocks(stream, state)) !== NO_MATCH) return result;
  if (state.zeroRule !== "rule19" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("(:)(?!>)|(->)", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule19"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "((?:one|lone|some|set)\\b)|(?=\\S)(?!/\\*|//|--)", endCaptures: {"1": "keyword"}, style: null, contentStyle: null, scan: scanContext19, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"1": "punctuation", "2": "operator"}, null) : RESTART;
  }
}
  if ((m = matchAt("(?<=:\\s*)(one|lone|some|set)\\b", stream, state))) return emitMatch(stream, state, m, {}, "keyword");
  if ((result = scanPunctuation(stream, state)) !== NO_MATCH) return result;
  if ((m = matchAt("(?:\\b(disj|var)\\b(?!\\$))|(?:\\b(one|lone|some|set)\\b(?=(?:(?:\\s+)|/\\*[\\s\\S]*?\\*/|//[^\\r\\n]*|--[^\\r\\n]*)*->))", stream, state))) return emitMatch(stream, state, m, {}, "keyword");
  if ((m = matchAt("(?:\\b(and|or|not|implies|iff|else)\\b)|(?:\\b(all|no|one|some|lone)\\b)|(?:\\b(always|eventually|after|before|until|releases|since|triggered|historically|once)\\b)", stream, state))) return emitMatch(stream, state, m, {}, "operator");
  if (state.zeroRule !== "rule20" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("\\blet\\b", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule20"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "(?=\\|(?!\\|)|\\{(?!\\s*[A-Za-z_][A-Za-z0-9_]*\\s*:))", endCaptures: {}, style: null, contentStyle: null, scan: scanMetaBindingLet, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "operator"}, null) : RESTART;
  }
}
  if ((m = matchAt("(?:\\b(in)\\b)|(?:\\b(sum|int)\\b)", stream, state))) return emitMatch(stream, state, m, {}, "operator");
  if ((m = matchAt("\\b(seq)\\b", stream, state))) return emitMatch(stream, state, m, {}, "keyword");
  if ((m = matchAt("(?:<=>|=>|!|&&|\\|\\||;)|(?:>>>|>>|<<)|(?:\\+\\+|->|<:|:>|\\+|-|&|\\*|\\^|~|\\.|#|')|(?:=<|>=|=|<|>)", stream, state))) return emitMatch(stream, state, m, {}, "operator");
  if ((result = scanMetaIdentifiers(stream, state)) !== NO_MATCH) return result;
  if ((result = scanKeywords(stream, state)) !== NO_MATCH) return result;
  if ((result = scanConstants(stream, state)) !== NO_MATCH) return result;
  if ((result = scanIdentifiers(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanPunctuation(stream, state) {
  let m, result;
  if ((m = matchAt("(?:\\.\\.)|(?:,|:(?!>)|(?<!\\|)\\|(?!\\|))|(?:@)", stream, state))) return emitMatch(stream, state, m, {}, "punctuation");
  return NO_MATCH;
}

function scanConstants(stream, state) {
  let m, result;
  if ((m = matchAt("(?:\\b0x(?:_|[0-9A-Fa-f]{2})+\\b)|(?:\\b0b[01_]+\\b)|(?:\\b[0-9][0-9_]*\\b)", stream, state))) return emitMatch(stream, state, m, {}, "number");
  if ((m = matchAt("\\b(univ|iden|none)\\b", stream, state))) return emitMatch(stream, state, m, {}, "atom");
  return NO_MATCH;
}

function scanIdentifiers(stream, state) {
  let m, result;
  if ((m = matchAt("\\b[A-Za-z_][A-Za-z0-9_]*\\b", stream, state))) return emitMatch(stream, state, m, {}, "variableName");
  return NO_MATCH;
}

function scanMetaSignatureHierarchy(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if ((result = scanStrings(stream, state)) !== NO_MATCH) return result;
  if ((result = scanMetaIdentifiers(stream, state)) !== NO_MATCH) return result;
  if ((m = matchAt("\\b(extends|in)\\b", stream, state))) return emitMatch(stream, state, m, {}, "keyword");
  if ((m = matchAt("(?:\\+)|(?:=)", stream, state))) return emitMatch(stream, state, m, {}, "operator");
  if ((result = scanKeywords(stream, state)) !== NO_MATCH) return result;
  if ((result = scanConstants(stream, state)) !== NO_MATCH) return result;
  if ((result = scanIdentifiers(stream, state)) !== NO_MATCH) return result;
  if ((result = scanPunctuation(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanSignatureHierarchy(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule23" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("(?<=sig\\s*)", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule23"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "(?=\\{)", endCaptures: {}, style: null, contentStyle: null, scan: scanMetaSignatureHierarchy, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanMetaBlockSignatureFields(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if ((result = scanStrings(stream, state)) !== NO_MATCH) return result;
  if ((m = matchAt("\\b(private|var|disj)\\b(?!\\$)", stream, state))) return emitMatch(stream, state, m, {}, "keyword");
  if ((result = scanFieldDeclarationValue(stream, state)) !== NO_MATCH) return result;
  if ((result = scanBlockExpressions(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

function scanSignatureFields(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule24" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("\\{", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule24"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "\\}", endCaptures: {"0": "punctuation"}, style: null, contentStyle: null, scan: scanMetaBlockSignatureFields, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"0": "punctuation"}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanFieldDeclarationValue(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule25" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("(:)(?!>)|(=)", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule25"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "(?=,|\\})", endCaptures: {}, style: null, contentStyle: null, scan: scanMetaDeclarationFieldValue, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"1": "punctuation", "2": "operator"}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanCallableParameterValue(stream, state) {
  let m, result;
  if (state.zeroRule !== "rule26" || state.zeroPosition !== stream.pos) {
  if ((m = matchAt("(:)(?!>)|(=)", stream, state))) {
    if (!m[0].length) { state.zeroRule = "rule26"; state.zeroPosition = stream.pos; }
    state.contexts.push({end: "(?=,|\\])", endCaptures: {}, style: null, contentStyle: null, scan: scanMetaDeclarationParameterValue, openingStream: stream, openingPosition: stream.pos});
    return m[0].length ? emitMatch(stream, state, m, {"1": "punctuation", "2": "operator"}, null) : RESTART;
  }
}
  return NO_MATCH;
}

function scanRoot(stream, state) {
  let m, result;
  if ((result = scanComments(stream, state)) !== NO_MATCH) return result;
  if ((result = scanStrings(stream, state)) !== NO_MATCH) return result;
  if ((result = scanModuleDeclarationsAndImports(stream, state)) !== NO_MATCH) return result;
  if ((result = scanSignatureDeclarations(stream, state)) !== NO_MATCH) return result;
  if ((result = scanCallableDeclarationParameters(stream, state)) !== NO_MATCH) return result;
  if ((result = scanTopLevelLetDeclarations(stream, state)) !== NO_MATCH) return result;
  if ((result = scanDeclarationKeywords(stream, state)) !== NO_MATCH) return result;
  if ((result = scanMetaIdentifiers(stream, state)) !== NO_MATCH) return result;
  if ((result = scanKeywords(stream, state)) !== NO_MATCH) return result;
  if ((result = scanBlockExpressions(stream, state)) !== NO_MATCH) return result;
  if ((result = scanExpressionBrackets(stream, state)) !== NO_MATCH) return result;
  if ((result = scanExpressionBlocks(stream, state)) !== NO_MATCH) return result;
  if ((result = scanPunctuation(stream, state)) !== NO_MATCH) return result;
  if ((result = scanConstants(stream, state)) !== NO_MATCH) return result;
  if ((result = scanIdentifiers(stream, state)) !== NO_MATCH) return result;
  return NO_MATCH;
}

const scanMetaBindingMacro10 = scanMetaBindingMacro;
const scanMetaParametersModule = scanContext8;
const scanMetaBlock = scanMetaGroup;
const scanMetaDeclarationFieldValue = scanMetaGroup;
const scanMetaDeclarationParameterValue = scanMetaGroup;

function tagFor(style) {
  return style && tokenTable[style] ? style : null;
}

function startState() {
  return {contexts: [], pending: [], delimiterDepth: 0, history: "", zeroRule: null, zeroPosition: -1};
}

function copyState(state) {
  return {contexts: state.contexts.slice(), pending: state.pending.slice(), delimiterDepth: state.delimiterDepth, history: state.history, zeroRule: state.zeroRule, zeroPosition: state.zeroPosition};
}

function matchAt(pattern, stream, state) {
  const expression = new RegExp(pattern, "dy");
  if (!pattern.includes("(?<=")) {
    expression.lastIndex = stream.pos;
    return expression.exec(stream.string);
  }
  const prefix = state.history;
  expression.lastIndex = prefix.length;
  const match = expression.exec(prefix + stream.string.slice(stream.pos));
  if (!match) return null;
  match.index = 0;
  match.indices = match.indices.map(span => span && [span[0] - prefix.length, span[1] - prefix.length]);
  return match;
}

function queueMatch(state, match, captures, fallbackScope) {
  const text = match[0];
  const indices = match.indices || [];
  const ranges = [];
  const boundaries = new Set([0, text.length]);
  for (const [index, capture] of Object.entries(captures || {})) {
    const span = indices[Number(index)];
    const tag = tagFor(capture);
    if (!span || !tag) continue;
    const from = span[0] - match.index;
    const to = span[1] - match.index;
    ranges.push({from, to, tag});
    boundaries.add(from);
    boundaries.add(to);
  }
  const points = [...boundaries].sort((a, b) => a - b);
  for (let i = 0; i < points.length - 1; i++) {
    const from = points[i], to = points[i + 1];
    let tag = tagFor(fallbackScope);
    for (const range of ranges) {
      if (range.from <= from && range.to >= to) tag = range.tag;
    }
    state.pending.push({length: to - from, tag, text: text.slice(from, to)});
  }
}

function emitMatch(stream, state, match, captures, style) {
  queueMatch(state, match, captures, style);
  return state.pending.length ? emitPending(stream, state) : null;
}

function emitPending(stream, state) {
  const part = state.pending.shift();
  stream.pos += part.length;
  state.history = (state.history + part.text).slice(-64);
  state.zeroRule = null;
  state.zeroPosition = -1;
  if (part.tag === "punctuation") {
    if (/^[([{]$/.test(part.text)) state.delimiterDepth++;
    else if (/^[)\]}]$/.test(part.text)) state.delimiterDepth = Math.max(0, state.delimiterDepth - 1);
  }
  return part.tag;
}

function token(stream, state) {
  if (state.pending.length) return emitPending(stream, state);
  while (state.contexts.length && state.contexts.at(-1).end === "$" && stream !== state.contexts.at(-1).openingStream) {
    state.contexts.pop();
  }
  const spaceContext = state.contexts.at(-1);
  if (stream.eatSpace()) {
    state.history = (state.history + stream.current()).slice(-64);
    return tagFor(spaceContext?.contentStyle || spaceContext?.style);
  }

  while (state.contexts.length && state.contexts.at(-1).end === "(?<=\\})"
         && (stream !== state.contexts.at(-1).openingStream || stream.pos !== state.contexts.at(-1).openingPosition)
         && state.history.trimEnd().endsWith("}")) {
    state.contexts.pop();
  }
  const context = state.contexts.at(-1);
  if (context && (stream !== context.openingStream || stream.pos !== context.openingPosition)) {
    const ending = matchAt(context.end, stream, state);
    if (ending) {
      state.contexts.pop();
      if (ending[0].length) {
        queueMatch(state, ending, context.endCaptures, context.style);
        return emitPending(stream, state);
      }
      return token(stream, state);
    }
  }

  const result = context ? context.scan(stream, state) : scanRoot(stream, state);
  if (result === RESTART) return token(stream, state);
  if (result !== NO_MATCH) return result;

  if (stream.eol()) return null;
  stream.next();
  state.history = (state.history + stream.current()).slice(-64);
  return tagFor(context?.contentStyle || context?.style);
}

function indent(state, textAfter, context) {
  const closes = /^[})\]]/.test(textAfter);
  return Math.max(0, state.delimiterDepth - (closes ? 1 : 0)) * context.unit;
}

const tokenTable = {
  comment: tags.comment,
  docComment: tags.docComment,
  string: tags.string,
  number: tags.number,
  constant: tags.constant(tags.variableName),
  typeName: tags.typeName,
  atom: tags.atom,
  variableName: tags.variableName,
  punctuation: tags.punctuation,
  operator: tags.operator,
  keyword: tags.keyword,
};

export const alloy = StreamLanguage.define({
  name: "alloy",
  startState,
  copyState,
  token,
  indent,
  languageData: {commentTokens: {line: "//", block: {open: "/*", close: "*/"}}},
  tokenTable,
});

export default alloy;
