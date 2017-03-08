
EPSILON = 'epsilon'
INSTANCE_ID = 'InstanceID'
ALG_NAME='AlgName'
BASIC_PAC_CONDITION='pacCondition'
EXPANDED = 'Expanded'


results_path = '../../../results'
bc_root = "%s/boundedCost" % results_path
open_root = "%s/openBased" % results_path
dps_root  = "%s/dps" % results_path
basic_root = "%s/basic" % results_path

basic_file = 'conditions-%s-gains.csv'
bc_file = 'bc-%s.csv'
open_file='open-%s.csv'
dps_file='DPS%s.csv'




def merge_to_one():




    domains = ['GridPathFinding','pancakes', 'dockyard', 'vacuumrobot']

    for domain in domains:
        # Read basic
        basic = Data('Basic')
        infile = basic_root + '/' + (basic_file % domain)
        basic.load(infile)

        # Read open-based
        open = Data('Open')
        infile = open_root + '/' + (open_file % domain)
        open.load(infile )

        # read dps
        dps = Data('DPS')
        infile = dps_root + '/' + (dps_file % domain)
        dps.load(infile )

        # read bounded-cost
        bc = Data('BC')
        infile = bc_root + '/' + (bc_file % domain)
        bc.load(infile)


        merged = merge([basic,open,dps,bc])

        compute_gains(merged)

        out_file = '%s\merged-%s.csv' % (results_path,domain)
        merged.save(out_file)


        # compute gains with DPS
        # compute gains with FMin


class Data:
    def __init__(self, name):
        self.name = name
        self.data = []
    def set_headers(self,headers):
        self.headers = headers
    def add_data_line(self, data_line):
        record = dict()
        for i in xrange(len(self.headers)):
            record[self.headers[i]]=data_line[i]
        self.data.append(record)

    def load(self,file_name):
        infile = open(file_name,'r')
        first_line = True
        for line in infile.readlines():
            if first_line:
                self.set_headers([x.strip() for x in line.split(",")])
                first_line=False
            else:
                self.add_data_line([x.strip() for x in line.split(",")])

    def save(self,file_name):
        outfile= open(file_name,'w')
        outfile.write(",".join(self.headers))
        outfile.write("\n")

        for record in self.data:
            to_print = [record[header] for header in self.headers]
            out_line = ""
            for value in to_print:
                out_line+=","+str(value)
            out_line= out_line[1:] # remove the first comma
            outfile.write(out_line)
            outfile.write("\n")

def merge(datasets):
    all_headers = set()
    for data in datasets:
        print data.name
        for header in data.headers:
            all_headers.add(header)

    all_headers.add("AlgName")

    mergedData = Data("Merged")
    mergedData.set_headers(all_headers)

    print all_headers

    for dataset in datasets:
        for record in dataset.data:
            new_record = dict()
            for header in all_headers:
                if header=='AlgName':
                    new_record[header]=dataset.name
                elif header in dataset.headers:
                    new_record[header]=record[header]
                else:
                    new_record[header]='N/A'

            if new_record['AlgName']=='DPS':
                new_record[EPSILON]=float(record['weight'])-1
                new_record[EPSILON]=str(round(new_record[EPSILON], 2)) # To avoid floating point issues
                new_record.pop('weight',None)

            new_record[EPSILON]=float(new_record[EPSILON])
            new_record[EXPANDED]=int(new_record[EXPANDED])

            mergedData.data.append(new_record)

    mergedData.headers.remove('weight')
    return mergedData




def compute_gains(merged_data):
    key_to_expanded_of = dict()
    for record in merged_data.data:
        key = (record[EPSILON],record[INSTANCE_ID])
        if(key_to_expanded_of.has_key(key)==False):
            key_to_expanded_of[key]=dict()

        expanded_of = key_to_expanded_of[key]

        # Check FMin
        if record[ALG_NAME]=='Basic' and record[BASIC_PAC_CONDITION]=='org.cs4j.core.algorithms.pac.FMinCondition':
            expanded_of['FMin']=record[EXPANDED]
        if record[ALG_NAME]=='Basic' and record[BASIC_PAC_CONDITION]=='org.cs4j.core.algorithms.pac.OraclePACCondition':
            expanded_of['Oracle']=record[EXPANDED]
        if record[ALG_NAME]=='DPS':
            expanded_of['DPS']=record[EXPANDED]


    for record in merged_data.data:
        key = (record[EPSILON],record[INSTANCE_ID])
        expanded_of = key_to_expanded_of[key]
        record['GainFMin'] =float(expanded_of['FMin'])/ float(record[EXPANDED])
        record['GainOracle'] =float(expanded_of['Oracle'])/ float(record[EXPANDED])
        record['GainDPS'] = float(expanded_of['DPS'])/float(record[EXPANDED])
        record['HasGainFMin'] = record['GainFMin']>1
        record['HasGainOracle'] = record['GainOracle']>1
        record['HasGainDPS'] = record['GainDPS']>1

    merged_data.headers=merged_data.headers.union(['GainFMin','GainOracle','GainDPS','HasGainFMin','HasGainOracle','HasGainDPS'])

merge_to_one()


