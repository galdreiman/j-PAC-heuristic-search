


import os



def compute_all_gains():
    compute_gains("dockyard")
    compute_gains("pancakes")
    compute_gains("vacuumrobot")
    compute_gains("GridPathFinding")


def compute_gains(domain):
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
    # 9 pacCondition
    # 10 delta
    # 11 epsilon
    # 12 pacCondition2
    # 13 gain
    # 14 has gain

    #domain = "pancakes"
    #domain = "pancakes"
    #domain = "vacuumrobot"
    print "##### Input"
    epsilons = []
    input = open('../../../results/conditions-%s.csv' % domain,'r')
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
        delta = float(data_line[10])
        condition = data_line[12]
        # Store delta zero value for every experiment
        if delta==0.0 and "FMin" in condition:
            epsilon = float(data_line[11])
            if epsilon not in epsilons:
                epsilons.append(epsilon)
            key = (data_line[0],data_line[11])
            to_best[key]=data_line[6] # Store expanded

        data.append(data_line)
    input.close()

    # Compute gains
    print "##### Output "
    output = open('../../../results/summary/openbased/conditions-%s-gains.csv' % domain,'w')
    output.write("%s, expandedFMin\n" % headers)
    for data_line in data:

        key = (data_line[0],data_line[10])
        output_line = ",".join(data_line)
        print output_line
        gain = float(data_line[6])/float(to_best[key])
        if gain>1:
            has_gain=True
        else:
            has_gain=False

        output.write("%s,%s,%s,%s\n" % (output_line,to_best[key], gain, has_gain ))

    output.close()

compute_all_gains()
