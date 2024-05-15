import json
import os
import re

with open('method_logs.log', 'r') as f:
    data = f.readlines()

methods = {}
for line in data:
    # Make a regex with the pattern of the data
    # This regex will be used to extract the duration, method name, method params, and method return type
    # The format is [duration] method_name (method_params) method_return_type
    # Example: [29300] com.package.class.method (params) int
    regex = re.compile(r'\[(\d+)\] ([\w\W.]+) \(([\w\W, ]*)\) ([\w\W]+)')
    match = regex.match(line)

    # If the regex matches the line, extract the data
    if match:
        duration = int(match.group(1))
        method_name = match.group(2)
        method_params = match.group(3)
        method_return_type = match.group(4)

        # If the method is already in the dictionary, add the duration to the total
        if method_name in methods:
            methods[method_name]['total_duration'] += duration
            methods[method_name]['count'] += 1
        # If the method is not in the dictionary, add it with the duration
        else:
            methods[method_name] = {
                'total_duration': duration,
                'count': 1,
                'params': method_params,
                'return_type': method_return_type.replace('\n', '')
            }
    else:
        print(f'No match: {line}')
        
# Sort the methods by total duration
sorted_methods = sorted(methods.items(), key=lambda x: x[1]['total_duration'], reverse=True)

# Write the sorted methods to a JSON file
with open('sorted_methods.json', 'w') as f:
    json.dump(sorted_methods, f, indent=4)