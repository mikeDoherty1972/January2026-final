import io
p='app/src/main/java/com/security/app/ScadaActivity.kt'
with io.open(p,'r',encoding='utf-8') as f:
    lines=f.readlines()
nest=0
first_zero=None
zero_lines=[]
for i,l in enumerate(lines, start=1):
    opens=l.count('{')
    closes=l.count('}')
    prev=nest
    nest += opens - closes
    if prev>0 and nest==0 and first_zero is None:
        first_zero=i
    if nest==0:
        zero_lines.append(i)

print('total_lines=',len(lines))
print('final_nesting=',nest)
print('first_line_where_nesting_became_0=', first_zero)
print('count_zero_lines=', len(zero_lines))
print('showing first 40 lines where nesting==0:')
for ln in zero_lines[:40]:
    print(ln, lines[ln-1].rstrip())

