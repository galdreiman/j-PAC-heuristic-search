


import os




def compute_gains():
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
    domain = "GridPathFinding"
    print "##### Input"
    epsilons = []
    input = open('../../../results/summary/conditions-%s.csv' % domain,'r')
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
        delta = float(data_line[9])

        # Store delta zero value for every experiment
        if (delta==0.0):
            epsilon = float(data_line[10])
            if epsilon not in epsilons:
                epsilons.append(epsilon)
            key = (data_line[0],data_line[10],data_line[11])
            to_best[key]=data_line[6] # Store expanded

        data.append(data_line)
    input.close()

    # Compute gains
    print "##### Output "
    output = open('../../../results/conditions-%s-gains.csv' % domain,'w')
    output.write("%s, expandedFMin\n" % headers)
    for data_line in data:

        key = (data_line[0],data_line[10],data_line[11])
        output_line = ",".join(data_line)
        print output_line
        output.write("%s,%s\n" % (output_line,to_best[key]))

    output.close()

compute_gains()
