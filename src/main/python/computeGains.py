


import os



def compute_all_gains():
    input_path_template = '../../../results/boundedCost/%s-together.csv'
    output_path_template = '../../../results/boundedCost/conditions-%s-gains.csv'
    compute_gains(input_path_template % 'dockyard', output_path_template % 'dockyard')
    #compute_gains(input_path_template % "pancakes")
    #compute_gains(input_path_template % "vacuumrobot")
    #compute_gains(input_path_template % "GridPathFinding")


def compute_gains(input_path, output_path):
    #  Input line format:
    # 0 InstanceID
    # 1 Found
    # 2 Depth
    # 3 Cost
    # 4 Iterations
    # 5 Generated
    # 6 Expanded
    # 7 Cpu Time
    # 8 Wall Time
    # 9 delta
    # 10 epsilon
    # 11 pacCondition

    #domain = "pancakes"
    #domain = "pancakes"
    #domain = "vacuumrobot"

    INSTANCE_INDEX =0
    DELTA_INDEA = 9
    EPSILON_INDEX = 10
    EXPANDED_INDEX = 6
    CONDITION_INDEX = 11

    print "##### Input"
    epsilons = []
    input = open(input_path,'r')
    first_line = True
    data = []
    to_best = dict()
    for line in input.readlines():
        # Remove headers line
        if first_line:
            first_line=False
            headers = line.strip()
            continue

        # Get delta
        parts = line.split(",")
        data_line = [x.strip() for x in parts]
        delta = float(data_line[DELTA_INDEA])
        condition = data_line[CONDITION_INDEX]
        # Store delta zero value for every experiment
        if delta==0.0 and "DPS" in condition:
            epsilon = float(data_line[EPSILON_INDEX])
            if epsilon not in epsilons:
                epsilons.append(epsilon)
            key = (data_line[INSTANCE_INDEX],data_line[EPSILON_INDEX])
            to_best[key]=data_line[EXPANDED_INDEX] # Store expanded
        print data_line
        data.append(data_line)
    input.close()

    # Compute gains
    print "##### Output "
    output = open(output_path,'w')
    output.write("%s, expandedFMin\n" % headers)
    for data_line in data:

        key = (data_line[INSTANCE_INDEX],data_line[EPSILON_INDEX])
        output_line = ",".join(data_line)
        print output_line
        gain = float(data_line[EXPANDED_INDEX])/float(to_best[key])
        if gain>1:
            has_gain=True
        else:
            has_gain=False

        output.write("%s,%s,%s,%s\n" % (output_line,to_best[key], gain, has_gain ))

    output.close()

compute_all_gains()
