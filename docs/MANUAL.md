# Manual

## Introduction
This tool validates a fastq file or a pair of fastq files.

## Example
To run this tool:
```bash
java -jar ValidateFastq-version.jar -i input.fq
```

To get help:
```bash
java -ja ValidateFastq-version.jar --help
Usage: ValidateFastq [options]

  -l <value> | --log_level <value>
        Level of log information printed. Possible levels: 'debug', 'info', 'warn', 'error'
  -h | --help
        Print usage
  -v | --version
        Print version
  -i <file> | --fastq1 <file>

  -j <file> | --fastq2 <file>
```

## Output
An error if something is amiss.
