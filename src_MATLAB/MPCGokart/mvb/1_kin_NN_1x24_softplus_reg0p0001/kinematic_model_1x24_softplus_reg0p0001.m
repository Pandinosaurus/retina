function [ACCX,ACCY,ACCROTZ] = kinematic_model_1x24_softplus_reg0p0001(VELX,VELY,VELROTZ,BETA,AB,TV,param)
    VELROTZ_T = VELX*tan(BETA)/1.19;
    k = 2.2;

    dVELROTZ = k * (VELROTZ_T - VELROTZ);
    
    ACCX_NOM = AB;
    ACCY_NOM = 0;
    ACCROTZ_NOM = dVELROTZ;
    
    w1 = [-0.34190255 -1.2844857 -0.036699913 -0.8184676 0.93070376 -0.2785573 -0.91852754 0.15700023 -0.47477037 1.1448227 1.3627032 0.40704957 -1.6859477 1.2985144 -0.9741509 -0.96603036 -0.057240356 0.6079143 0.17798683 1.0027503 -1.2596043 0.4337728 -1.1825589 0.9228809;0.49923933 0.060214832 0.10655002 1.4964646 0.77768487 -0.53152704 0.7075897 0.061303858 -0.9301197 0.28309816 -0.17716843 -0.7579263 0.8680295 0.51465046 0.21694249 -0.24901886 -0.015247705 -0.3069475 0.74757046 -0.4406191 -0.2105842 1.0689114 -0.7669676 -0.38610533;-2.065662 0.9018266 0.31641763 -0.3988293 -1.4786633 -0.7984463 0.5539725 -1.1118829 -0.20953287 -0.48703226 0.8789111 -0.335917 -0.62040263 -0.08358714 -0.31059813 -0.86563236 -0.34378624 1.0924219 0.15949923 -0.47164214 1.8337736 -0.37496027 -0.038235553 -1.0892533;0.9259439 -0.6964226 -0.045709588 -0.2022913 0.7740749 2.3213692 -1.0268129 -0.24983639 1.9624267 -0.31828687 -0.5270342 0.16118051 -1.4501766 -1.4110149 -0.26409972 0.057472777 -0.39099714 -0.040409055 -0.32738954 1.5433569 -0.631606 -0.14745463 2.2589734 0.75040185;0.08565039 0.16080633 0.4586553 -0.080411404 0.28113165 0.41561532 0.09801514 0.21904704 0.030206902 0.10695533 -0.16088404 -0.37764978 -0.3393016 -0.36977497 -0.17741495 -0.6064545 0.12983015 0.96012694 -0.7019749 0.12218669 0.4278331 0.3206423 -0.011439274 0.19356197;-0.114039734 0.0018564853 0.34086844 1.0294304 -0.12263906 0.382873 0.14832908 -0.42745686 -0.2286686 -0.10699281 0.017154174 -0.41412374 0.13406882 -0.066514954 0.05924063 -0.1815133 0.018010208 0.32715145 0.12354135 -0.08277862 0.1259497 0.99866706 -0.03191178 -0.36425924];
    b1 = [-1.8547744 1.7755111 0.12566349 0.4853331 -0.40258843 0.8172247 1.4473387 1.4625522 1.4706377 -2.8882773 -2.6205435 0.12141936 0.69770545 0.061770055 1.2180835 -1.0409274 -0.07539023 1.017294 0.1702946 -2.2145865 -0.626053 0.57725406 0.063017316 0.42088497];
    w2 = [-0.16595109 1.064521 -2.4654415;0.77314025 1.5465636 -1.0652462;0.5603029 -0.008026892 -0.027958147;0.13529533 0.07139332 -0.98485327;-0.16639785 -0.98569393 0.9074941;-0.0048397984 0.040131908 1.5711244;0.08924691 -1.2964519 -0.71914536;0.0099097015 -0.88023853 0.79292256;-0.3655009 -0.18696804 1.3601831;-0.6626383 2.257554 0.2598941;0.24515627 -1.4769562 1.7250267;-0.2640919 0.6759293 -0.4400323;-0.0160849 0.74415714 1.4124761;-0.100324795 0.05727456 -1.2606655;-1.2202694 -0.7191613 -0.19010764;1.0142876 -1.0112497 -0.05935963;-0.3821479 -0.03902635 -0.25719446;-0.19202304 0.4226258 -1.1585662;0.2863878 0.24832168 0.69778585;-0.45017383 -1.043263 -1.6513243;-0.5791173 -0.8338508 1.5202134;-0.08235124 -0.053794127 1.1951871;0.28907207 0.1064438 -1.880989;0.7777469 0.9632865 -0.20280246];
    b2 = [0.0060713035 -0.11812817 -0.4622028];
    means = [2.6855843572110367 0.0006739826858158322 -0.04802685383934488 -0.027302772888852182 0.06560439433747117 -0.0741416072412400];
    stds = [1.9181710264256149 0.3073995994274385 0.5524821241332127 0.20117397363021836 0.837672127818587 0.634702899104761];

    input = [VELX,VELY,VELROTZ,BETA,AB,TV];
    normed_input = (input - means) ./ stds;
    
    h1 = log(exp(normed_input * w1 + b1) + 1);
    disturbance = h1 * w2 + b2;
    
    ACCX = ACCX_NOM + disturbance(1);
    ACCY = ACCY_NOM + disturbance(2);
    ACCROTZ = ACCROTZ_NOM + disturbance(3);
end
