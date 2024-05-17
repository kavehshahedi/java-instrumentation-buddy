import json
import os
import re

with open('method_logs.log', 'r') as f:
    data = f.readlines()

output = []
for line in data:
    # Make a regex with the pattern of the data
    # This regex will be used to extract the duration, method name, method params, and method return type
    # The format is [duration] method_name (method_params) method_return_type
    # Example: [29300] ENTER|EXIT com.package.class.method (params) int
    regex = re.compile(r'\[(\d+)\] (ENTER|EXIT) ([\w\W\s.]+)')
    match = regex.match(line)

    # If the regex matches the line, extract the data
    if match:
        timestamp = int(match.group(1))
        eventType = match.group(2)
        method = match.group(3).strip()

        output.append({
            'ts': timestamp,
            'ph': 'B' if eventType == 'ENTER' else 'E',
            'name': f'{method}'
        })
    else:
        print(f'No match: {line}')

# Write the sorted methods to a JSON file
with open('trace.json', 'w') as f:
    json.dump(output, f, indent=4)