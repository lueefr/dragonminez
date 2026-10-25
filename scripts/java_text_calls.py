"""Small Java lexical helpers: balanced call arguments, including multiline expressions."""
import re

def arguments(source, start):
    result=[];last=start;depth=0;quote=None;escaped=False
    for index in range(start,len(source)):
        char=source[index]
        if quote:
            if escaped:escaped=False
            elif char=='\\':escaped=True
            elif char==quote:quote=None
        elif char in '\"\'':quote=char
        elif char in '([{':depth+=1
        elif char in ')]}':
            if char==')' and depth==0:
                result.append((last,index,source[last:index].strip()))
                return result,index
            depth-=1
        elif char==',' and depth==0:
            result.append((last,index,source[last:index].strip()));last=index+1
    raise ValueError('Unclosed Java call')

STRING=re.compile(r'"(?:\\.|[^"\\])*"')
FORMAT=re.compile(r'%(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?[a-zA-Z%]')

def statement(source,start):
    """Read an initializer without mistaking punctuation inside strings for its end."""
    quote=None;escaped=False;depth=0
    for index in range(start,len(source)):
        char=source[index]
        if quote:
            if escaped:escaped=False
            elif char=='\\':escaped=True
            elif char==quote:quote=None
        elif char in '\"\'':quote=char
        elif char in '([{':depth+=1
        elif char in ')]}':depth-=1
        elif char==';' and depth==0:return source[start:index]
    return ''

def embedded_labels(expression,prefix,seen=frozenset()):
    """Follow local String/component initializers into rendering calls; leave runtime data alone."""
    if len(seen)>8:return []
    # A translated component is a resolved text source; replace() arguments describe data cleanup.
    edits=[]
    for match in re.finditer(r'(?:Component\.translatable|ConfigTranslations\.text|\btr|\btxt|\.(?:replace(?:All|First)?|equals(?:IgnoreCase)?|contains|startsWith|endsWith|matches))\(',expression):
        _,end=arguments(expression,match.end());edits.append((match.start(),end+1))
    for start,end in sorted(edits,reverse=True):expression=expression[:start]+' RESOLVED '+expression[end:]
    found=[]
    for match in STRING.finditer(expression):
        import json
        value=json.loads(match[0]);residue=FORMAT.sub('',value)
        if re.search('[a-zA-Z]',residue):found.append(value)
    without=STRING.sub('',expression)
    for match in re.finditer(r'(?<![\w.])([a-zA-Z_]\w*)\b',without):
        name=match[1]
        if name in seen or without[match.end():].lstrip().startswith('('):continue
        declarations=list(re.finditer(r'\b(?:String|Component|MutableComponent|var)\s+'+re.escape(name)+r'\s*=\s*',prefix))
        if declarations:
            declaration=declarations[-1]
            found.extend(embedded_labels(statement(prefix,declaration.end()),prefix[:declaration.start()],seen|{name}))
    return found
