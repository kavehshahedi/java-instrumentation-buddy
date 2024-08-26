import json
import re
import argparse

REGEX = re.compile(r'\[(\d+)\] (S|E) ([\w\W\s.]+)')

def process_line(line):
    match = REGEX.match(line)
    if match:
        timestamp = int(match.group(1))
        eventType = match.group(2)
        method_signature = match.group(3).strip()
        return {
            'ts': timestamp,
            'ph': 'B' if eventType == 'S' else 'E',
            'name': method_signature
        }
    else:
        return None

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('input', help='Input file')
    parser.add_argument('output', help='Output file')
    parser.add_argument('--batch_size', type=int, default=1000, help='Number of lines to process in each batch')
    args = parser.parse_args()
    
    with open(args.input, 'r') as f, open(args.output, 'w') as out_f:
        out_f.write('[\n')  # Start of JSON array
        
        batch = []
        first = True

        for line in f:
            result = process_line(line)
            if result:
                batch.append(result)
                
                if len(batch) >= args.batch_size:
                    if not first:
                        out_f.write(',\n')
                    else:
                        first = False

                    out_f.write(',\n'.join(json.dumps(item, indent=4) for item in batch))
                    batch.clear()
        
        # Write any remaining items in the batch
        if batch:
            if not first:
                out_f.write(',\n')
            out_f.write(',\n'.join(json.dumps(item, indent=4) for item in batch))
        
        out_f.write('\n]')  # End of JSON array

if __name__ == '__main__':
    main()